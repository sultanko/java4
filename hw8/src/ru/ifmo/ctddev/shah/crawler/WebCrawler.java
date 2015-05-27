package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Phaser;

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
     * @throws IOException when error while downloading occured
     */
    @Override
    public Result download(String url, int depth) {
        final Phaser phaser = new Phaser(1);
        final Map<String, Pair<Document, Integer>> result = new HashMap<>();
        final Map<String, IOException> errors = new HashMap<>();
        webData.downloadLink(result, phaser, errors, url, depth);
        phaser.arriveAndAwaitAdvance();
        Set<String> urls = result.keySet();
        urls.removeAll(errors.keySet());
        return new Result(new ArrayList<>(urls), errors);
    }

    /**
     * Close all working threads.
     */
    @Override
    public void close() {
        webData.close();
    }
}
