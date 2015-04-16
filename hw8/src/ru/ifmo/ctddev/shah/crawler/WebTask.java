package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.*;
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
    private final Set<String> result;
    private final AtomicInteger countTasks;
    private final Lock counterLock = new ReentrantLock();
    private final Condition isDone = counterLock.newCondition();

    private final ConcurrentOptional<IOException> exceptionThread;


    /**
     * @param webData object with thread pools
     * @param url url to download
     * @param maxDepth max depth of downloaded links
     */
    public WebTask(WebData webData, String url, int maxDepth) {
        this.webData = webData;
        this.result = new ConcurrentSkipListSet<>();
        countTasks = new AtomicInteger();
        exceptionThread = new ConcurrentOptional<>();
    }

    /**
     * Download all links from url by depth.
     * @return list of unique downloaded urls
     */
    public List<String> download(String url, int maxDepth) throws IOException{
        try {
            counterLock.lock();
            result.add(url);
            webData.downloadThreads.submit(new DownloaderWorker(url, maxDepth));
            while (countTasks.get() > 0) {
                    isDone.await();
            }
            webData.clearMap();
        } catch (InterruptedException ignored) {
        } finally {
            counterLock.unlock();
        }
        if (exceptionThread.isPresent()) {
            throw exceptionThread.get();
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
                if (!exceptionThread.isPresent())
                {
                    webData.acquire(url);
                    Document document = webData.downloader.download(url);
                    webData.release(url);
                    if (curDepth > 1) {
                        webData.extractorThreads.submit(new ExectractorWorker(document, curDepth - 1));
                    }
                }
            } catch (IOException e) {
                exceptionThread.set(e);
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
                if (!exceptionThread.isPresent()) {
                    List<String> links = document.extractLinks();
                    for (String link : links) {
                        if (result.add(link)) {
                            webData.downloadThreads.submit(new DownloaderWorker(link, curDepth));
                        }
                    }
                }
            } catch (IOException e) {
                exceptionThread.set(e);
            } finally {
                countDown();
            }
        }
    }
}
