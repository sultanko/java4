package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final AtomicInteger countTasks;
    private final Lock counterLock = new ReentrantLock();
    private final Condition isDone = counterLock.newCondition();
    private final ConcurrentMap<String, Pair<Document, Integer>> downloadedLinks;

    private volatile Optional<IOException> exceptionThread;


    /**
     * @param webData object with thread pools
     */
    public WebTask(WebData webData) {
        this.webData = webData;
        countTasks = new AtomicInteger();
        exceptionThread = Optional.empty();
        downloadedLinks = new ConcurrentHashMap<>();
    }

    /**
     * Download all links from url by depth.
     * @return list of unique downloaded urls
     */
    public List<String> download(String url, int maxDepth) throws IOException{
        List<String> result = new ArrayList<>();
        try {
            counterLock.lock();
//            result.add(url);
            downloadedLinks.put(url, new Pair<>(null, maxDepth));
            webData.addNewLink(url, maxDepth);
            webData.downloadThreads.submit(new DownloaderWorker());
            while (countTasks.get() > 0) {
                    isDone.await();
            }
            result.addAll(downloadedLinks.keySet());
            webData.clearMap(result);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            counterLock.unlock();
        }
        if (exceptionThread.isPresent()) {
            throw exceptionThread.get();
        }
        return result;
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

        private DownloaderWorker() {
            countTasks.incrementAndGet();
        }

        @Override
        public void run() {
            try {
                if (!exceptionThread.isPresent())
                {
//                    System.err.println("NOW WORKING " + countTasks);
                    Pair<String, Integer> now = webData.acquire();
//                    System.err.println("DOWNLOADING ACUIRED " + now.getKey() + "DEPTH " + now.getValue());
                    Document document = webData.downloader.download(now.getKey());
//                    System.err.println("DOWNLOADED " + now.getKey());
                    webData.release(now.getKey());
                    synchronized (downloadedLinks) {
                        now = new Pair<>(now.getKey(), downloadedLinks.get(now.getKey()).getValue());
                        downloadedLinks.replace(now.getKey(), new Pair<>(document, now.getValue()));
                    }
//                    System.err.println("DOWNLOADING RELEASED " + now.getKey());
                    if (now.getValue() > 1) {
                        webData.extractorThreads.submit(new ExectractorWorker(document, now.getValue() - 1));
                    }
                }
            } catch (IOException e) {
                exceptionThread = Optional.of(e);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
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
                        if (downloadedLinks.putIfAbsent(link, new Pair<>(null, curDepth)) == null) {
                            webData.addNewLink(link, curDepth);
                            webData.downloadThreads.submit(new DownloaderWorker());
                        } else if (downloadedLinks.get(link).getValue() == 1 && curDepth > 1){
                            synchronized (downloadedLinks) {
                                Document linkDoc = downloadedLinks.get(link).getKey();
                                if (linkDoc != null) {
                                    webData.extractorThreads.submit(
                                            new ExectractorWorker(downloadedLinks.get(link).getKey(), curDepth - 1));
                                }
                                downloadedLinks.replace(link, new Pair<>(linkDoc, curDepth));
                            }
//                                System.err.println("FAIL ADD " + link + " " + curDepth);
                        }
                    }
                }
            } catch (IOException e) {
                exceptionThread = Optional.of(e);
            } finally {
                countDown();
            }
        }
    }
}
