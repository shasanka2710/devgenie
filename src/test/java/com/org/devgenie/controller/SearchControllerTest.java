package com.org.devgenie.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class SearchControllerTest {
    @Test
    void performSemanticSearch_returnsResponse() {
        SearchController controller = new SearchController();
        Map<String, String> req = Map.of("prompt", "test");
        var resp = controller.performSemanticSearch(req);
        assert resp.getBody().get("response").contains("LLM response for prompt: test");
    }
}
