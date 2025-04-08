package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

public class CrawlResult {
    private String domain;
    private Set<String> productUrls;

    public CrawlResult(String domain, Set<String> productUrls) {
        this.domain = domain;
        this.productUrls = productUrls;
    }

    public String getDomain() {
        return domain;
    }

    public Set<String> getProductUrls() {
        return productUrls;
    }

    public String toJson() throws Exception {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
}