package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by sultan on 06.04.15.
 */
public class WebTask {
    private final WebData webData;
    private final String url;
    private final int maxDepth;
    private final Queue<String> result;
    private final AtomicInteger countTasks;

    public WebTask(WebData webData, String url, int maxDepth) {
        this.webData = webData;
        this.url = url;
        this.maxDepth = maxDepth;
        this.result = new ConcurrentLinkedQueue<>();
        countTasks = new AtomicInteger();
    }

    public List<String> download() {
        try {
            webData.downloadThreads.submit(new DownloaderWorker(url, 1)).get();
            while (countTasks.get() > 0) {
            }
        } catch (ExecutionException | InterruptedException ignored) {
        }
        return new ArrayList<>(result);
    }

    private class DownloaderWorker implements Runnable {
        private final String url;
        private final int curDepth;

        private DownloaderWorker(String url, int curDepth) {
            this.url = url;
            this.curDepth = curDepth;
            countTasks.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                webData.acquire(url);
                Document document = webData.downloader.download(url);
                webData.release(url);
                if (curDepth < maxDepth) {
                    webData.extractorThreads.submit(new ExectractorWorker(document, curDepth + 1));
                } else {
                    result.add(url);
                }
            } catch (IOException ignored) {
            } finally {
                countTasks.decrementAndGet();
            }
        }
    }

    private class ExectractorWorker implements Runnable {
        private final Document document;
        private final int curDepth;

        private ExectractorWorker(Document document, int curDepth) {
            this.document = document;
            this.curDepth = curDepth;
            countTasks.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                List<String> links = document.extractLinks();
                for (String link : links) {
                    webData.downloadThreads.submit(new DownloaderWorker(link, curDepth));
                }
            } catch (IOException ignored) {
            } finally {
                countTasks.decrementAndGet();
            }
        }
    }
}
