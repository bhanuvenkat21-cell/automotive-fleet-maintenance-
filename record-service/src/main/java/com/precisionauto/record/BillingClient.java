package com.precisionauto.record;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Record -> Billing hop, resolved through Eureka and load-balanced. */
@Component
public class BillingClient {
    private final RestClient client;

    public BillingClient(RestClient.Builder loadBalancedBuilder) {
        this.client = loadBalancedBuilder.baseUrl("http://billing-service").build();
    }

    public Dtos.InvoiceResponse generate(Dtos.InvoiceRequest request) {
        try {
            return client.post().uri("/invoices").body(request).retrieve().body(Dtos.InvoiceResponse.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "billing-service unavailable: " + e.getMessage());
        }
    }
}
