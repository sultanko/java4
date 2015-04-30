package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Result;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created on 06.04.15.
 * Support class for {@link WebCrawler}
 @author sultan
 */
class WebTask {
    private final WebData webData;
    private final Phaser phaser = new Phaser(1);
    private final ConcurrentMap<String, Pair<Document, Integer>> result = new ConcurrentHashMap<>();

    private final Map<String, IOException> errors;


    /**
     * @param webData object with thread pools
     */
    public WebTask(WebData webData) {
        this.webData = webData;
        errors = new ConcurrentHashMap<>();
    }

    /**
     * Download all links from url by depth.
     * @return list of unique downloaded urls
     */
    public Result download(String url, int maxDepth) {
        webData.downloadLink(result, phaser, errors, url, maxDepth);
        phaser.arriveAndAwaitAdvance();
        Set<String> urls = result.keySet();
        urls.removeAll(errors.keySet());
        return new Result(new ArrayList<>(urls), errors);
    }

}
