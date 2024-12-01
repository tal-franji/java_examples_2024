package il.ac.tau.cs.experiment;

import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiCrawler {
    static final String wikiBase = "https://www.wikipedia.org/";
    static final int maxPathLength = 100;
    ConcurrentHashMap<String, Boolean> visitedURLs = new ConcurrentHashMap<>();
    public boolean wasVisited(String url) {
        return visitedURLs.getOrDefault(url, false);
    }
    public void setVisited(String url) {
        visitedURLs.put(url, true);
    }
    public List<String> getWikiLinks(String url) throws InterruptedException {
        if (!url.contains("wikipedia.org")) {
            throw new IllegalArgumentException("URL is not a wikipedia URL");
        }
        String content = null;
        URLConnection connection = null;
        for (int retry = 0; retry < 3; retry++) {
            try {
                connection = new URL(url).openConnection();
                Scanner scanner = new Scanner(connection.getInputStream());
                scanner.useDelimiter("\\Z");
                content = scanner.next();
                scanner.close();
                break;
            } catch (java.io.IOException webException) {
                // Assuming this is a DDos prevention/rate lmiting error - slow down requests:
                Thread.sleep(100);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        if (content == null) {
            return new ArrayList<>();
        }
        var wikiLinks = new ArrayList<String>();
        Matcher m = Pattern.compile("href=\"/(wiki/[^\":]+)\"")
                .matcher(content);
        while (m.find()) {
            var urlToAdd = wikiBase + m.group(1);
            if (!wikiLinks.contains(urlToAdd)) {
                wikiLinks.add(urlToAdd);
            }
        }
        return wikiLinks;
    }
    // the games NaraView and WikiRace tye to find a path
    static class WikiPath {
        List<String> urls = new ArrayList<>();
        public WikiPath cloneAndAdd(String url) {
            var newPath = new WikiPath();
            // clone the array to a new array that can be modified by other threads
            newPath.urls = new ArrayList<>(this.urls);
            newPath.urls.add(url);
            return newPath;
        }
        public String lastUrl() {return this.urls.getLast();}
        public int length() {return urls.size();}
    }

    java.util.concurrent.LinkedBlockingQueue<WikiPath> pathsToExpandQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();
    java.util.concurrent.LinkedBlockingQueue<WikiPath> winningPathQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    class ExpanderThread extends Thread {
        String endUrlSuffix;

        public ExpanderThread(String endUrlSuffix) {
            this.endUrlSuffix = endUrlSuffix;
        }

        @Override
        public void run() {
            while (true) {
                WikiPath nextPath = null;
                try {
                    nextPath = pathsToExpandQueue.poll(3000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    System.out.println("DEBUG>>>> Exithing thread");
                    break;  // no more data in queue
                }
                System.out.println("DEBUG>>>> depth:" + Integer.toString(nextPath.length()) + " - " + nextPath.lastUrl());
                if (nextPath.urls.size() >= maxPathLength) {
                    System.out.println("DEBUG>>>> path max");
                    continue;  // ignore paths that are too long
                }
                List<String> links = null;
                try {
                    links = WikiCrawler.this.getWikiLinks(nextPath.lastUrl());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (var link : links) {
                    WikiCrawler.this.wasVisited(link);
                    if (link.endsWith(endUrlSuffix)) {
                        var winningPath = nextPath.cloneAndAdd(link);
                        winningPathQueue.add(winningPath);
                        System.out.println("DEBUG>>>> path FOUND");
                        return;
                    }
                    if (!WikiCrawler.this.wasVisited(link)) {
                        WikiCrawler.this.setVisited(link);
                        var newPath = nextPath.cloneAndAdd(link);
                        pathsToExpandQueue.add(newPath);
                    }
                }
            }
        }
    }

    public List<String> wikiRace(String startUrl, String endUrlSuffix) {
        var pathZero = new WikiPath().cloneAndAdd(startUrl);
        pathsToExpandQueue.add(pathZero);
        int nThreads = 10;
        for (int i = 0; i < nThreads; i++) {
            var t = new ExpanderThread(endUrlSuffix);
            t.setDaemon(true);  // This is needed so threads are stopped to allow main() to finish
            t.start();
        }
        try {
            WikiPath winningPath = WikiCrawler.this.winningPathQueue.poll(3, TimeUnit.HOURS);
            System.out.println("DEBUG>>>>> done.");
            return winningPath.urls;
        } catch (InterruptedException e) {
            throw new RuntimeException("Timeout - could not find winning path");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var crawler = new WikiCrawler();
        var startUrl = "https://en.wikipedia.org/wiki/Elephant";
        var endUrlSuffix = "/wiki/Israel";
        var winning = crawler.wikiRace(startUrl, endUrlSuffix);
        for (var l: winning) {
            System.out.println(l);
        }
    }
}
