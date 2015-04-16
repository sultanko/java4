package ru.ifmo.ctddev.shah.crawler;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

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
    public final ConcurrentMap<String, Semaphore> hosts;

    public WebData(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = downloaders;
        this.extractors = extractors;
        this.perHost = perHost;
        this.downloadThreads = Executors.newFixedThreadPool(downloaders);
        this.extractorThreads = Executors.newFixedThreadPool(extractors);
        hosts = new ConcurrentHashMap<>();
    }

    /**
     * Stop all threads.
     */
    public void close() {
        downloadThreads.shutdown();
        extractorThreads.shutdown();
    }

    /**
     * Acquires permit from semaphore for host of url
     * @param url url
     */
    public void acquire(String url) {
        try {
            String host = URLUtils.getHost(url);
            if (!hosts.containsKey(host)) {
                hosts.put(host, new Semaphore(perHost));
            }
            hosts.get(host).acquire();
        } catch (MalformedURLException | InterruptedException ignored) {
        }
    }

    /**
     * Release permit from semaphore for host of url
     * @param url url
     */
    public void release(String url) {
        try {
            String host = URLUtils.getHost(url);
            hosts.get(host).release();
        } catch (MalformedURLException ignored) {
        }
    }

    /**
     * Remove hosts from map if this isn't downloading now
     */
    public void clearMap() {
        synchronized (hosts) {
            Iterator<Map.Entry<String, Semaphore>> iterator = hosts.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Semaphore> entry = iterator.next();
                if (entry.getValue().availablePermits() == perHost) {
                    iterator.remove();
                }
            }
        }
    }
}
