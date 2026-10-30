package com.precisionauto.billing;

import com.precisionauto.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class InvoiceApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    static final String BODY = """
            {"recordId":77,"customerName":"Alice","vehicleReg":"KA01AB1234","laborHours":2,
             "parts":[{"name":"Oil filter","quantity":1,"unitPrice":500},{"name":"Spark plug","quantity":2,"unitPrice":250}]}""";

    @Test
    void generatesInvoiceIdempotentlyAndMarksPaid() throws Exception {
        String tech = "Bearer " + jwt.generate("tech1", "TECHNICIAN");
        String client = "Bearer " + jwt.generate("alice", "CLIENT");

        mvc.perform(post("/invoices").header("Authorization", client).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());

        mvc.perform(post("/invoices").header("Authorization", tech).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.subtotal").value(2600.00))
                .andExpect(jsonPath("$.taxAmount").value(468.00))
                .andExpect(jsonPath("$.total").value(3068.00))
                .andExpect(jsonPath("$.status").value("ISSUED"));

        // replay for the same record returns the same invoice, not a duplicate
        mvc.perform(post("/invoices").header("Authorization", tech).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
        mvc.perform(get("/invoices/record/77").header("Authorization", client)).andExpect(status().isOk());

        Long id = 1L;
        mvc.perform(patch("/invoices/" + id + "/pay").header("Authorization", tech))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    }
}
