package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.*;

/**
 * Created by sultan on 02.04.15.
 */
public class WebCrawler implements Crawler {
    private final WebData webData;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        webData = new WebData(downloader, downloaders, extractors, perHost);
    }

    @Override
    public List<String> download(String url, int depth) throws IOException {
        WebTask webTask = new WebTask(webData, url, depth);
        return webTask.download();
    }

    @Override
    public void close() {
        webData.close();
    }
}
