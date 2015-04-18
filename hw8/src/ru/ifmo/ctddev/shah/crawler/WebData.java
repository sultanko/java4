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
    public final ConcurrentMap<Integer, NavigableSet<String>> nowAvailableHosts;
    public final AtomicInteger firstHost;
    public final ConcurrentMap<String, Integer> hostDownloaders;
    public final ConcurrentMap<String, BlockingQueue< Pair<String, Integer>> > availableLinks;
    public final Lock hostAddLock = new ReentrantLock();
    public final Condition workWithLink = hostAddLock.newCondition();

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
        nowAvailableHosts = new ConcurrentHashMap<>();
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
//        System.err.println("ACUIRE " + firstHost);
//        System.err.println("HOSTS AVALAIBLE 0 " + nowAvailableHosts.get(0));
//        System.err.println("HOSTS AVALAIBLE 1 " + nowAvailableHosts.getOrDefault(1, new ConcurrentSkipListSet<>()));
        synchronized (firstHost) {
            while (firstHost.get() < perHost) {
                if (nowAvailableHosts.containsKey(firstHost.get()) &&
                        !nowAvailableHosts.get(firstHost.get()).isEmpty()) {
                    break;
                }
                firstHost.incrementAndGet();
            }
            while (firstHost.get() >= perHost) {
                firstHost.wait();
            }
//            System.err.println("ACUIRE " + (nowAvailableHosts.get(firstHost.get()) == null));
            String host = nowAvailableHosts.get(firstHost.get()).pollFirst();
//            System.err.println("FOR " + host + " COUNT " + availableLinks.get(host).size());
            int counter = firstHost.get() + 1;
            hostDownloaders.replace(host, counter);
//            System.err.println("ACUIRE LINKS " + availableLinks.get(host));
            if (availableLinks.get(host).size() > 1) {
                nowAvailableHosts.putIfAbsent(counter, new ConcurrentSkipListSet<>());
                nowAvailableHosts.get(counter).add(host);
            }
//            System.err.println("ACUIRE UPDATE " + firstHost.get());
            return availableLinks.get(host).poll();
        }
    }

    /**
     * Release permit from semaphore for host of url
     * @param url url
     */
    public void release(String url) throws MalformedURLException {
//        System.err.println("RELEASE " + url);
        String host = URLUtils.getHost(url);
//        System.err.println("HOST " + host);
        synchronized (firstHost) {
            int counter = hostDownloaders.get(host) - 1;
            hostDownloaders.replace(host, counter);
//            System.err.println("FOR " + host + " COUNT " + availableLinks.get(host).size());
//            System.err.println("HOST DOWNLOADERS WAS " + (counter + 1));
//            System.err.println("AVALAIBLE " + nowAvailableHosts.get(counter + 1));
            if (nowAvailableHosts.containsKey(counter + 1) && nowAvailableHosts.get(counter + 1).remove(host)) {
                if (!nowAvailableHosts.containsKey(counter)) {
                    nowAvailableHosts.putIfAbsent(counter, new ConcurrentSkipListSet<>());
                }
                nowAvailableHosts.get(counter).add(host);
                if (counter < firstHost.get()) {
                    firstHost.set(counter);
                    firstHost.notifyAll();
                }
            }
        }
    }

    public void addNewLink(String link, int curDepth) throws MalformedURLException {
//        System.err.println("ADD LINK " + link + " " + curDepth);
        String host = URLUtils.getHost(link);
        synchronized (firstHost) {
            if (!availableLinks.containsKey(host)) {
                availableLinks.putIfAbsent(host, new LinkedBlockingQueue<>());
            }
            availableLinks.get(host).add(new Pair<>(link, curDepth));
//            System.err.println("FOR " + host + " COUNT " + availableLinks.get(host).size());
            if (availableLinks.get(host).size() == 1) {
                hostDownloaders.putIfAbsent(host, 0);
                int countHost = hostDownloaders.get(host);
                nowAvailableHosts.putIfAbsent(countHost, new ConcurrentSkipListSet<>());
                nowAvailableHosts.get(countHost).add(host);
//                System.err.println("ADDDED HOST " + host + " TO " + countHost);
                if (countHost < firstHost.get()) {
                    firstHost.set(countHost);
                    firstHost.notifyAll();
                }
            }
        }
    }

    /**
     * Remove hosts from map if this isn't downloading now
     */
    public synchronized void clearMap() {
        Iterator<Map.Entry<String, BlockingQueue<Pair<String, Integer>>>> iterator = availableLinks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BlockingQueue<Pair<String, Integer>>> entry = iterator.next();
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
        Iterator<Map.Entry<String, Integer>> it2 = hostDownloaders.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<String, Integer> entry = it2.next();
            if (entry.getValue() == 0) {
                it2.remove();
            }
        }
    }
}
