package ru.ifmo.ctddev.shah.crawler;

import com.sun.istack.internal.NotNull;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import javafx.util.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created on 06.04.15.
 * Support class for {@link WebCrawler}.
 @author sultan
 */
class WebData {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloadThreads;
    private final ExecutorService extractorThreads;
    private final Map<String, Pair<Integer,
            Queue<Runnable>>> hosts;

    public WebData(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloadThreads = new ThreadPoolExecutor(downloaders, downloaders, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.extractorThreads = new ThreadPoolExecutor(extractors, extractors, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        hosts = new HashMap<>();
    }

    /**
     * Stop all threads.
     */
    public void close() {
        downloadThreads.shutdownNow();
        extractorThreads.shutdownNow();
    }

    /**
     * Download all links from given url by depth.
     * @param result map contains downloaded url and document for it
     * @param phaser {@link Phaser} for tasks count
     * @param errors save exception while downloading
     * @param url url to download
     * @param depth depth
     */
    public void  downloadLink(
            final Map<String, Pair<Document, Integer>> result,
            final Phaser phaser,
            final Map<String, IOException> errors,
            final String url, final Integer depth
    ) {
        result.put(url, new Pair<>(null, depth));
        addLinkToDownload(result, phaser, errors, url, depth);
    }

    private void checkNew(final String host) {
        synchronized (hosts) {
            final Pair<Integer, Queue<Runnable>> oldValue = hosts.get(host);
            Integer downloaders = oldValue.getKey() - 1;
            if (!oldValue.getValue().isEmpty()) {
                // start new downloader if exist
                downloadThreads.submit(oldValue.getValue().poll());
                downloaders++;
            }
            if (downloaders == 0) {
                // remove unused host from Map
                hosts.remove(host);
            } else {
                hosts.replace(host, new Pair<>(downloaders, oldValue.getValue()));
            }
        }
    }

    private void addLinkToDownload(
            final Map<String, Pair<Document, Integer>> result,
            final Phaser phaser,
            final Map<String, IOException> errors,
            final String url, final Integer depth
    ) {
        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            errors.put(url, e);
            return;
        }
        phaser.register();
        Runnable runnable = () -> {
            try {
                final Document document = downloader.download(url);
                final Pair<Document, Integer> now;
                synchronized (result) {
                    // get updated depth of url
                    now = result.replace(url, new Pair<>(document, depth));
                }
                final Integer curDepth = now.getValue();
                if (curDepth > 1) {
                    addLinkToExtract(result, phaser, errors, url, document, curDepth - 1);
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                checkNew(host);
                phaser.arrive();
            }
        };
        synchronized (hosts) {
            Pair<Integer, Queue<Runnable>> oldValue = hosts.get(host);
            // if host hasn't downloaded before
            // add it
            if (oldValue == null) {
                downloadThreads.submit(runnable);
                hosts.put(host, new Pair<>(1, new LinkedList<>()));
            } else {
                final Integer hostDownloaders = oldValue.getKey();
                if (hostDownloaders < perHost) {
                    downloadThreads.submit(runnable);
                    hosts.replace(host, new Pair<>(hostDownloaders + 1, oldValue.getValue()));
                } else {
                    // WebCrawler now downloading in maximum threads from this host
                    // therefore save this url for better time
                    oldValue.getValue().add(runnable);
                }
            }
        }
    }

    private void addLinkToExtract(final Map<String, Pair<Document, Integer>> result,
                                  final Phaser phaser,
                                  final Map<String, IOException> errors,
                                  final String url, final Document document, final Integer urlDepth) {
        phaser.register();
        extractorThreads.submit(() -> {
            try {
                final List<String> links = document.extractLinks();
                for (String extractedLink : links) {
                    synchronized (result) {
                        final Pair<Document, Integer> oldValue = result.get(extractedLink);
                        if (oldValue == null) {
                            // url hasn't added before => download it
                            addLinkToDownload(result, phaser, errors, extractedLink, urlDepth);
                            result.put(extractedLink, new Pair<>(null, urlDepth));
                        } else if (oldValue.getValue() < urlDepth) {
                            final Document downloadedDocument = oldValue.getKey();
                            if (downloadedDocument != null) {
                                // url has downloaded earlier => extract links from downloaded document
                                addLinkToExtract(result, phaser, errors, extractedLink, downloadedDocument, urlDepth);
                            }
                            result.replace(extractedLink, new Pair<>(downloadedDocument, urlDepth));
                        }
                    }
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arrive();
            }
        });
    }

}
