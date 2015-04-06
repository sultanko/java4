package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.net.MalformedURLException;
import java.util.concurrent.*;

/**
 * Created by sultan on 06.04.15.
 */
public class WebData {
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

    public void close() {
        downloadThreads.shutdown();
        extractorThreads.shutdown();
    }

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

    public void release(String url) {
        try {
            String host = URLUtils.getHost(url);
            hosts.get(host).release();
        } catch (MalformedURLException ignored) {
        }
    }
}
