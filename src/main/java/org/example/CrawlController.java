package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    @Autowired
    private CrawlerService crawlerService;

    @PostMapping
    public Map<String, Set<String>> crawl(@RequestBody CrawlRequest request) {
        return crawlerService.startCrawling(request.getUrls());
    }

}
