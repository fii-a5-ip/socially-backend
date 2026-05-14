package com.soccialy.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soccialy.backend.entity.Filter;
import com.soccialy.backend.repository.FilterRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingProcessControllerTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void process_retriesRateLimitAndNormalizesReturnedFilters() throws IOException {
        AtomicInteger attempts = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/onboardingProcess/", exchange -> {
            int attempt = attempts.incrementAndGet();
            String responseBody = attempt == 1
                    ? "{\"error\":\"Too Many Requests\"}"
                    : "{\"status\":\"complete\",\"current_filters\":[\"cafe\",\"27\",\"missing\"],\"final_filters\":[\"bar\",60]}";
            int status = attempt == 1 ? HttpStatus.TOO_MANY_REQUESTS.value() : HttpStatus.OK.value();
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.start();

        Filter cafe = new Filter();
        cafe.setId(60);
        cafe.setName("cafe");
        Filter bar = new Filter();
        bar.setId(27);
        bar.setName("bar");

        FilterRepository filterRepository = mock(FilterRepository.class);
        when(filterRepository.findByName("cafe")).thenReturn(Optional.of(cafe));
        when(filterRepository.findByName("bar")).thenReturn(Optional.of(bar));
        when(filterRepository.findByName("missing")).thenReturn(Optional.empty());

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        OnboardingProcessController controller = new OnboardingProcessController(
                baseUrl,
                filterRepository,
                new ObjectMapper()
        );

        var response = controller.process("{\"step\":3}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, attempts.get());
        assertEquals("{\"status\":\"complete\",\"current_filters\":[60,27],\"final_filters\":[27,60]}",
                response.getBody());
    }

    @Test
    void process_returnsBadGatewayWhenAiServiceUnavailable() {
        OnboardingProcessController controller = new OnboardingProcessController(
                "http://127.0.0.1:1",
                mock(FilterRepository.class),
                new ObjectMapper()
        );

        var response = controller.process("{\"step\":0}");

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("{\"error\":\"Onboarding AI service is unavailable.\"}", response.getBody());
    }
}
