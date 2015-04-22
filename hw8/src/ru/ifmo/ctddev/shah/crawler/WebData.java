package ru.ifmo.ctddev.shah.crawler;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import javafx.util.Pair;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    public final ConcurrentMap<String, Integer> hostDownloaders;
    public final ConcurrentMap<String, BlockingQueue< Pair<String, Integer>> > availableLinks;
    public final Lock hostLock = new ReentrantLock();
    public final Condition stateUpdated = hostLock.newCondition();

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
        nowAvailableHosts = new CopyOnWriteArrayList<>();
        nowAvailableHosts.add(new TreeSet<>());
        hostDownloaders = new ConcurrentHashMap<>();
        availableLinks = new ConcurrentHashMap<>();
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
    public Pair<String, Integer> acquire() throws InterruptedException {
        try {
            hostLock.lock();
            while (firstHost.get() < perHost) {
                if (!nowAvailableHosts.get(firstHost.get()).isEmpty()) {
                    break;
                }
                firstHost.incrementAndGet();
            }
            while (firstHost.get() >= perHost) {
                stateUpdated.await();
            }
            String host = nowAvailableHosts.get(firstHost.get()).pollFirst();
            int counter = firstHost.get() + 1;
            hostDownloaders.replace(host, counter);
            if (availableLinks.get(host).size() > 1) {
                if (nowAvailableHosts.size() <= counter) {
                    nowAvailableHosts.add(new ConcurrentSkipListSet<>());
                }
                nowAvailableHosts.get(counter).add(host);
            }
            return availableLinks.get(host).poll();
        } finally {
            hostLock.unlock();
        }
    }

    /**
     * Release permit from semaphore for host of url
     * @param url url
     */
    public void release(String url) throws MalformedURLException {
        String host = URLUtils.getHost(url);
        try {
            hostLock.lock();
            Integer counter;
            do {
                counter = hostDownloaders.replace(host, hostDownloaders.get(host) - 1);
            } while (counter == null);
            if (nowAvailableHosts.size() > counter
                && nowAvailableHosts.get(counter).remove(host)) {
                    counter--;
                    if (nowAvailableHosts.get(counter).add(host)) {
                        while (!firstHost.compareAndSet(firstHost.get(), Math.min(firstHost.get(), counter)))
                        {}
                            stateUpdated.signal();
                    }
            }
        } finally {
            hostLock.unlock();
        }
    }

    public void addNewLink(String link, int curDepth) throws MalformedURLException {
        String host = URLUtils.getHost(link);
        if (!availableLinks.containsKey(host)) {
            availableLinks.putIfAbsent(host, new LinkedBlockingQueue<>());
        }
        availableLinks.get(host).add(new Pair<>(link, curDepth));
        if (availableLinks.get(host).size() == 1) {
            hostDownloaders.putIfAbsent(host, 0);
            try {
                hostLock.lock();
                int countHost = hostDownloaders.get(host);
                if (nowAvailableHosts.size() <= countHost) {
                    nowAvailableHosts.add(new TreeSet<>());
                }
                if (nowAvailableHosts.get(countHost).add(host)) {
                    if (countHost < firstHost.get()) {
                        firstHost.set(countHost);
                    }
                    stateUpdated.signal();
                }
            } finally {
                hostLock.unlock();
            }
        }
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
