package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created on 06.04.15.
 * Support class for {@link WebCrawler}
 @author sultan
 */
class WebTask {
    private final WebData webData;
    private final ConcurrentMap<String, Pair<Document, Integer>> downloadedLinks;
    private final Phaser phaser = new Phaser(1);
    private final ConcurrentMap<String, Pair<Document, Integer>> result = new ConcurrentHashMap<>();

    private final AtomicReference<IOException> exceptionThread;


    /**
     * @param webData object with thread pools
     */
    public WebTask(WebData webData) {
        this.webData = webData;
        exceptionThread = new AtomicReference<>(null);
        downloadedLinks = new ConcurrentHashMap<>();
    }

    /**
     * Download all links from url by depth.
     * @return list of unique downloaded urls
     */
    public List<String> download(String url, int maxDepth) throws IOException{
        result.put(url, new Pair<>(null, maxDepth));
        webData.addNewLink(result, phaser, exceptionThread, url, maxDepth);
        phaser.arriveAndAwaitAdvance();
        webData.clearMap(result.keySet());
//        if (exceptionThread.get() != null) {
//            throw exceptionThread.get();
//        }
        return new ArrayList<>(result.keySet());
    }

}
