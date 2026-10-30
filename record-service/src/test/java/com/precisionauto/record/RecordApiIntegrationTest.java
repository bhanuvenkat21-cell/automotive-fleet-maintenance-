package com.precisionauto.record;

import com.precisionauto.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class RecordApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean BillingClient billing;

    static final String BODY = """
            {"appointmentId":42,"vehicleReg":"MH12XY0001","customerName":"Bob","workDescription":"Clutch replaced",
             "technician":"tech1","laborHours":3,"parts":[{"name":"Clutch kit","quantity":1,"unitPrice":5000}]}""";

    @Test
    void technicianCreatesRecordAndClientReadsVehicleHistory() throws Exception {
        when(billing.generate(any())).thenReturn(new Dtos.InvoiceResponse(1L, "INV-1", new BigDecimal("8968.00")));
        String tech = "Bearer " + jwt.generate("tech1", "TECHNICIAN");
        String client = "Bearer " + jwt.generate("bob", "CLIENT");

        mvc.perform(post("/records").header("Authorization", client).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post("/records").header("Authorization", tech).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.invoiceNumber").value("INV-1"));
        // replay is idempotent
        mvc.perform(post("/records").header("Authorization", tech).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        mvc.perform(get("/records/vehicle/mh12xy0001").header("Authorization", client))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workDescription").value("Clutch replaced"));
    }

    @Test
    void historyWithoutTokenIs401() throws Exception {
        mvc.perform(get("/records/vehicle/ANY")).andExpect(status().isUnauthorized());
    }
}
