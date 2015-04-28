package ru.ifmo.ctddev.shah.crawler;

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
    private final ConcurrentMap<String, Pair<Integer,
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

    public void dowloadLink(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final AtomicReference<IOException> reference,
                                   final String url, final Integer depth) throws MalformedURLException {
        addLinkToDownload(result, phaser, reference, url, depth);
    }

    private void checkNew(final String host) {
        hosts.computeIfPresent(host, (key, oldValue) -> {
            Integer downloaders = oldValue.getKey() - 1;
            if (!oldValue.getValue().isEmpty()) {
                downloadThreads.submit(oldValue.getValue().poll());
                downloaders++;
            }
            return new Pair<>(downloaders, oldValue.getValue());
        });
    }

    private void addLinkToDownload(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final AtomicReference<IOException> reference,
                                  final String url, final Integer depth) throws MalformedURLException {
        if (result.putIfAbsent(url, new Pair<>(null, depth)) != null) {
            return;
        }
        phaser.register();
        final String host = URLUtils.getHost(url);
        Runnable runnable = () -> {
            try {
                final Document document = downloader.download(url);
                final Pair<Document, Integer> now = result.replace(url, new Pair<>(document, depth));
                final Integer curDepth = now.getValue();
                if (curDepth > 1) {
                    addLinkToExtract(result, phaser, reference, document, curDepth - 1);
                }
            } catch (IOException e) {
                reference.set(e);
            } finally {
                phaser.arrive();
                checkNew(host);
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

    private void addLinkToExtract(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final AtomicReference<IOException> reference,
                                 final Document document, final Integer urlDepth) {
        phaser.register();
        extractorThreads.submit(() -> {
            try {
                final List<String> links = document.extractLinks();
                for (String extractedLink : links) {
                    result.computeIfPresent(extractedLink, (key, oldValue) -> {
                        // oldValue.getValue() - previous depth of extractedLink
                        if (oldValue.getValue() < urlDepth) {
                            final Document dowloadedDocument = oldValue.getKey();
                            if (dowloadedDocument != null) {
                                addLinkToExtract(result, phaser, reference, dowloadedDocument, urlDepth);
                            } else {
                                return new Pair<>(null, urlDepth);
                            }
                        }
                        return oldValue;
                    });
                    addLinkToDownload(result, phaser, reference, extractedLink, urlDepth);
                }
            } catch (IOException e) {
                reference.set(e);
            } finally {
                phaser.arrive();
            }
        });
    }

    /**
     * Remove hosts from map if this isn't downloading now
     */
    public void removeDownloaded(Collection<String> links) throws MalformedURLException {
        for (String link : links) {
            String host = URLUtils.getHost(link);
            hosts.computeIfPresent(host, (key, oldValue) -> {
                if (oldValue.getKey() == 0 && oldValue.getValue().isEmpty()) {
                    return null;
                }
                return oldValue;
            });
        }
    }
}
