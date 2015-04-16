package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.*;

/**
 * Created on 02.04.15.
 * Provide implementation of {@link Crawler} interface.
 @author sultan
 */
public class WebCrawler implements Crawler {
    private final WebData webData;

    /**
     * @param downloader using for download url
     * @param downloaders maximum count of downloaded urls at the same time
     * @param extractors maximum count of extracting urls at the same time
     * @param perHost maximum count of downloaded urls from host at the same time
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        webData = new WebData(downloader, downloaders, extractors, perHost);
    }

    /**
     * Download url and all links from url.
     * @param url url to download
     * @param depth max depth of links to download
     * @return list of downloaded links
     * @throws IOException
     */
    @Override
    public List<String> download(String url, int depth) throws IOException {
        WebTask webTask = new WebTask(webData, url, depth);
        return webTask.download(url, depth);
    }

    /**
     * Close all working threads.
     */
    @Override
    public void close() {
        webData.close();
    }
}
