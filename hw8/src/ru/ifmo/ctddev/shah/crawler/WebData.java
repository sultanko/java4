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
    public final ConcurrentMap<String, Pair<Integer,
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
        hosts = new ConcurrentHashMap<>();
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
     * @param reference save exception while downloading
     * @param url url to download
     * @param depth depth
     */
    public void downloadLink(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final Map<String, IOException> reference,
                             final String url, final Integer depth) {
        result.put(url, new Pair<>(null, depth));
        addLinkToDownload(result, phaser, reference, url, depth);
    }

    private void checkNew(final String host) {
        hosts.computeIfPresent(host, (key, oldValue) -> {
            Integer downloaders = oldValue.getKey() - 1;
            if (!oldValue.getValue().isEmpty()) {
                downloadThreads.submit(oldValue.getValue().poll());
                downloaders++;
            }
            // remove unused url from HashMap
            if (downloaders == 0) {
                return null;
            }
            return new Pair<>(downloaders, oldValue.getValue());
        });
    }

    private void addLinkToDownload(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final Map<String, IOException> errors,
                                  final String url, final Integer depth) {
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
                final Pair<Document, Integer> now = result.replace(url, new Pair<>(document, depth));
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
        hosts.compute(host, (key, oldValue) -> {
            if (oldValue == null) {
                downloadThreads.submit(runnable);
                return new Pair<>(1, new LinkedList<>());
            } else {
                final Integer hostDownloaders = oldValue.getKey();
                if (hostDownloaders < perHost) {
                    downloadThreads.submit(runnable);
                    return new Pair<>(hostDownloaders + 1, oldValue.getValue());
                } else {
                    oldValue.getValue().add(runnable);
                }
                return oldValue;
            }
        });
    }

    private void addLinkToExtract(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final Map<String, IOException> errors,
                                 final String url, final Document document, final Integer urlDepth) {
        phaser.register();
        extractorThreads.submit(() -> {
            try {
                final List<String> links = document.extractLinks();
                for (String extractedLink : links) {
                    result.compute(extractedLink, (key, oldValue) -> {
                        if (oldValue == null) {
                            addLinkToDownload(result, phaser, errors, extractedLink, urlDepth);
                            return new Pair<>(null, urlDepth);
                        }
                        // oldValue.getValue() - previous depth of extractedLink
                        if (oldValue.getValue() < urlDepth) {
                            final Document downloadedDocument = oldValue.getKey();
                            if (downloadedDocument != null) {
                                addLinkToExtract(result, phaser, errors, extractedLink, downloadedDocument, urlDepth);
                            } else {
                                return new Pair<>(null, urlDepth);
                            }
                        }
                        return oldValue;
                    });
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arrive();
            }
        });
    }

}
