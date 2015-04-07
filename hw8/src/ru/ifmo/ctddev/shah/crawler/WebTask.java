package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Created on 06.04.15.
 * Support class for {@link WebCrawler}
 @author sultan
 */
class WebTask {
    private final WebData webData;
    private final String url;
    private final int maxDepth;
    private final Queue<String> result;
    private final AtomicInteger countTasks;
    private final Lock counterLock = new ReentrantLock();
    private final Condition isDone = counterLock.newCondition();

    /**
     * @param webData object with thread pools
     * @param url url to download
     * @param maxDepth max depth of downloaded links
     */
    public WebTask(WebData webData, String url, int maxDepth) {
        this.webData = webData;
        this.url = url;
        this.maxDepth = maxDepth;
        this.result = new ConcurrentLinkedQueue<>();
        countTasks = new AtomicInteger();
    }

    /**
     * Download all links from url by depth.
     * @return list of downloaded urls
     */
    public List<String> download() {
        try {
            counterLock.lock();
            webData.downloadThreads.submit(new DownloaderWorker(url, 1));
            while (countTasks.get() > 0) {
                    isDone.await();
            }
        } catch (InterruptedException ignored) {
        } finally {
            counterLock.unlock();
        }
        return new ArrayList<>(result);
    }

    private void countDown() {
        if (countTasks.decrementAndGet() == 0) {
            try {
                counterLock.lock();
                isDone.signal();
            } finally {
                counterLock.unlock();
            }
        }
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
                result.add(url);
                if (curDepth < maxDepth) {
                    webData.extractorThreads.submit(new ExectractorWorker(document, curDepth + 1));
                }
            } catch (IOException ignored) {
            } finally {
                countDown();
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
                countDown();
            }
        }
    }
}
