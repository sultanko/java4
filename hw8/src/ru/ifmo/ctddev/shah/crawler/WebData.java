package ru.ifmo.ctddev.shah.crawler;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import javafx.util.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created on 06.04.15.
 * Support class for {@link WebCrawler}.
 @author sultan
 */
class WebData {
    public final Downloader downloader;
    public final int downloaders;
    public final int extractors;
    public final int perHost;
    public final ExecutorService downloadThreads;
    public final ExecutorService extractorThreads;
    public final List<NavigableSet<String>> nowAvailableHosts;
    public final AtomicInteger firstHost;
    public final Map<String, Integer> hostDownloaders;
    public final Map<String, BlockingQueue< Pair<String, Integer>> > availableLinks;

    public WebData(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = downloaders;
        this.extractors = extractors;
        this.perHost = perHost;
        this.downloadThreads = new ThreadPoolExecutor(downloaders, downloaders, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.extractorThreads = new ThreadPoolExecutor(extractors, extractors, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        nowAvailableHosts = new ArrayList<>();
        nowAvailableHosts.add(new TreeSet<>());
        hostDownloaders = new HashMap<>();
        availableLinks = new HashMap<>();
        firstHost = new AtomicInteger(perHost);
    }

    /**
     * Stop all threads.
     */
    public void close() {
        downloadThreads.shutdownNow();
        extractorThreads.shutdownNow();
    }

    /**
     * Acquires permit from semaphore for host of url
     */
    public synchronized Pair<String, Integer> acquire() throws InterruptedException {
        //            hostLock.lock();
        while (firstHost.get() < perHost) {
            if (!nowAvailableHosts.get(firstHost.get()).isEmpty()) {
                break;
            }
            firstHost.incrementAndGet();
        }
        while (firstHost.get() >= perHost) {
//                stateUpdated.await();
            wait();
        }
        String host = nowAvailableHosts.get(firstHost.get()).pollFirst();
        int counter = firstHost.get() + 1;
        hostDownloaders.replace(host, counter);
        if (availableLinks.get(host).size() > 1) {
            if (nowAvailableHosts.size() <= counter) {
                nowAvailableHosts.add(new TreeSet<>());
            }
            nowAvailableHosts.get(counter).add(host);
        }
        return availableLinks.get(host).poll();
    }

    /**
     * Release permit from semaphore for host of url
     * @param url url
     */
    public synchronized void release(String url) throws MalformedURLException {
        String host = URLUtils.getHost(url);
        Integer counter = hostDownloaders.replace(host, hostDownloaders.get(host) - 1);
        if (nowAvailableHosts.size() > counter
            && nowAvailableHosts.get(counter).remove(host)) {
                counter--;
                if (nowAvailableHosts.get(counter).add(host)) {
                    firstHost.set(Math.min(firstHost.get(), counter));
//                        stateUpdated.signal();
                    notify();
                }
        }
    }

    public synchronized void addNewLink(String link, int curDepth) throws MalformedURLException {
        String host = URLUtils.getHost(link);
        //            hostLock.lock();
        if (!availableLinks.containsKey(host)) {
            availableLinks.put(host, new LinkedBlockingQueue<>());
        }
        availableLinks.get(host).add(new Pair<>(link, curDepth));
        if (availableLinks.get(host).size() == 1) {
            hostDownloaders.putIfAbsent(host, 0);
            int countHost = hostDownloaders.get(host);
            if (nowAvailableHosts.size() <= countHost) {
                nowAvailableHosts.add(new TreeSet<>());
            }
            if (nowAvailableHosts.get(countHost).add(host)) {
                firstHost.set(Math.min(firstHost.get(), countHost));
//                    stateUpdated.signal();
                notify();
            }
        }
    }

    public void downloadNewLink(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final AtomicReference<IOException> reference) {
        downloadThreads.submit(() -> {
            try {
//                if (reference.get() != null) {
//                    return;
//                }
                Pair<String, Integer> now = acquire();
                String url = now.getKey();
                final Document document = downloader.download(url);
                release(url);
                final Integer urlDepth = now.getValue();
                if (urlDepth > 1) {
                    extractNewLink(result, phaser, reference, document, urlDepth);
                }

            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                reference.set(e);
            } finally {
                phaser.arrive();
            }
        });
    }

    public void extractNewLink(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final AtomicReference<IOException> reference,
                               final Document document, final Integer urlDepth) {
        phaser.register();
        extractorThreads.submit(() -> {
            try {
//                            if (reference.get() != null) {
//                                return;
//                            }
                List<String> links = document.extractLinks();
                for (String extractedLink : links) {
                    Pair<Document, Integer> oldValue = result.putIfAbsent(extractedLink, new Pair<>(null, urlDepth - 1));
                    if (oldValue == null) {
                        addNewLink(result, phaser, reference, extractedLink, urlDepth - 1);
                    } else if (oldValue.getValue() < urlDepth - 1) {
                        if (oldValue.getKey() != null) {
                            extractNewLink(result, phaser, reference, oldValue.getKey(), urlDepth);
                        }
                    }
                }
            } catch (IOException e) {
                reference.set(e);
            } finally {
                phaser.arrive();
            }
        });

    }

    public synchronized void addNewLink(final ConcurrentMap<String, Pair<Document, Integer>> result, final Phaser phaser, final AtomicReference<IOException> reference,
                                        String link, int curDepth) throws MalformedURLException {
        String host = URLUtils.getHost(link);
        phaser.register();
        availableLinks.putIfAbsent(host, new LinkedBlockingQueue<>());
        availableLinks.get(host).add(new Pair<>(link, curDepth));
        if (availableLinks.get(host).size() == 1) {
            hostDownloaders.putIfAbsent(host, 0);
            int countHost = hostDownloaders.get(host);
            if (nowAvailableHosts.size() <= countHost) {
                nowAvailableHosts.add(new TreeSet<>());
            }
            if (nowAvailableHosts.get(countHost).add(host)) {
                firstHost.set(Math.min(firstHost.get(), countHost));
                notify();
            }
        }
        downloadNewLink(result, phaser, reference);
    }

    /**
     * Remove hosts from map if this isn't downloading now
     */
    public synchronized void clearMap(Collection<String> links) throws MalformedURLException {
        for (String link : links) {
            String host = URLUtils.getHost(link);
            if (hostDownloaders.getOrDefault(host, -1) == 0
                    && availableLinks.get(host).isEmpty()) {
                availableLinks.remove(host);
                hostDownloaders.remove(host);
            }
        }
    }
}
