package ru.ifmo.ctddev.shah.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;

import java.io.IOException;
import java.util.List;

public class Main {

    private static void printUsage() {
        System.err.println("WebCrawler url downloads extractors perHost");
    }
    public static void main(String[] args) {
        if (args != null && args.length == 5) {
            for (String arg : args) {
                if (arg == null) {
                    printUsage();
                    return;
                }
            }
            if (!args[0].equals("WebCrawler")) {
                printUsage();
                return;
            }
            try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]))) {
                List<String> urls = webCrawler.download(args[1], 2);
                System.out.println(urls.size());
                for (String url : urls) {
                    System.out.println(url);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            printUsage();
        }
	// write your code here
    }
}
