package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class CrawlerService {

    private static final int MAX_THREADS = 10;
    private static final int MAX_DEPTH = 3;

    private final List<CrawlResult> crawlResults = new CopyOnWriteArrayList<>();

    private static final List<String> PRODUCT_PATTERNS = List.of("/product", "/p/", "/item");

    public Map<String, Set<String>> startCrawling(List<String> domains) {
        Map<String, Set<String>> results = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        for (String domain : domains) {
            results.put(domain, ConcurrentHashMap.newKeySet());
            executor.submit(() -> crawlDomain(domain, results.get(domain)));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return results;
    }

    private void crawlDomain(String baseUrl, Set<String> productUrls) {
        Set<String> visited = ConcurrentHashMap.newKeySet();
        Queue<UrlDepthPair> queue = new ConcurrentLinkedQueue<>();
        queue.add(new UrlDepthPair(baseUrl, 0));
        String absHref = null;

        while (!queue.isEmpty()) {
            UrlDepthPair current = queue.poll();
            if (current.depth > MAX_DEPTH || visited.contains(current.url)) continue;

            visited.add(current.url);
            try {
                Document doc = Jsoup.connect(current.url)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    absHref = link.absUrl("href");
                    if (!absHref.contains(new URL(baseUrl).getHost())) continue;

                    if (isProductUrl(absHref)) {
                        productUrls.add(absHref.split("[?#]")[0]);
                    }

                    queue.add(new UrlDepthPair(absHref, current.depth + 1));
                }

            } catch (Exception ignored) {
                System.err.println("Error crawling " + current.url + ": " + ignored.getMessage());
            }

            saveResult(extractDomain(baseUrl), productUrls);
        }
    }

    private void saveResult(String domain, Set<String> urls) {
        try {
            Files.createDirectories(Paths.get("output"));
            CrawlResult result = new CrawlResult(domain, urls);
            crawlResults.add(result);
            try (FileWriter writer = new FileWriter("output/" + domain + ".json")) {
                writer.write(result.toJson());
            }
            System.out.println("Saved results for " + domain);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost().replace("www.", "");
        } catch (Exception e) {
            throw new RuntimeException("Invalid URL: " + url);
        }
    }

    private boolean isProductUrl(String url) {
        return PRODUCT_PATTERNS.stream().anyMatch(url::contains);
    }

    private static class UrlDepthPair {
        String url;
        int depth;

        UrlDepthPair(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    public List<CrawlResult> getCrawlResults() {
        return crawlResults;
    }
}
