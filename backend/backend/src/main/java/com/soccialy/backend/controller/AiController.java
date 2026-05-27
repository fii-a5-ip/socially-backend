package com.soccialy.backend.controller;

import com.soccialy.backend.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/onboardingProcess")
public class AiController {

    private final AiService aiService;

    @Autowired
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> processOnboarding(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiService.processOnboarding(payload));
    }
}
