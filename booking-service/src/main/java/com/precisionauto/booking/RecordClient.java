package com.precisionauto.booking;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Booking -> Record hop. "record-service" is resolved via Eureka and load-balanced client-side. */
@Component
public class RecordClient {
    private final RestClient client;

    public RecordClient(RestClient.Builder loadBalancedBuilder) {
        this.client = loadBalancedBuilder.baseUrl("http://record-service").build();
    }

    public Dtos.RecordResponse createRecord(Dtos.CreateRecordRequest request) {
        try {
            return client.post().uri("/records").body(request).retrieve().body(Dtos.RecordResponse.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "record-service unavailable: " + e.getMessage());
        }
    }
}
