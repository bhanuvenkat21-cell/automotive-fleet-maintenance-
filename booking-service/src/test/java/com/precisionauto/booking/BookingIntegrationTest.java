package com.precisionauto.booking;

import com.precisionauto.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class BookingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean RecordClient recordClient;

    String client() { return "Bearer " + jwt.generate("alice", "CLIENT"); }
    String tech()   { return "Bearer " + jwt.generate("tech1", "TECHNICIAN"); }

    String booking(int bay, LocalDateTime start, int minutes) {
        return """
               {"vehicleReg":"KA01AB1234","customerName":"Alice","serviceType":"General service",
                "bayNumber":%d,"startTime":"%s","durationMinutes":%d}""".formatted(bay, start, minutes);
    }

    static LocalDateTime slot(int day, int hour) {
        return LocalDateTime.now().plusDays(day).withHour(hour).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void requestWithoutTokenIs401() throws Exception {
        mvc.perform(get("/bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    void doubleBookingSameBayIs409_butAdjacentAndOtherBayAreOk() throws Exception {
        LocalDateTime s = slot(10, 9);
        mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(1, s, 120))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SCHEDULED"));
        mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(1, s.plusHours(1), 60))).andExpect(status().isConflict());
        mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(2, s, 120))).andExpect(status().isCreated());
        mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(1, s.plusHours(2), 60))).andExpect(status().isCreated());
    }

    @Test
    void availabilityExcludesBookedBay() throws Exception {
        LocalDateTime s = slot(11, 9);
        mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(3, s, 60))).andExpect(status().isCreated());
        mvc.perform(get("/bookings/availability").header("Authorization", client())
                        .param("start", s.toString()).param("end", s.plusMinutes(30).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void clientCannotComplete_technicianCan() throws Exception {
        String created = mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(4, slot(12, 9), 60))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));
        String complete = """
                {"workDescription":"Oil + filter","laborHours":1.5,"parts":[{"name":"Filter","quantity":1,"unitPrice":250}]}""";

        mvc.perform(post("/bookings/" + id + "/complete").header("Authorization", client())
                .contentType(MediaType.APPLICATION_JSON).content(complete)).andExpect(status().isForbidden());

        when(recordClient.createRecord(any())).thenReturn(new Dtos.RecordResponse(7L, 8L, "INV-20300101-00008", new BigDecimal("1652.00")));
        mvc.perform(post("/bookings/" + id + "/complete").header("Authorization", tech())
                        .contentType(MediaType.APPLICATION_JSON).content(complete))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-20300101-00008"));
    }

    @Test
    void clientCannotSeeSomeoneElsesBooking() throws Exception {
        String created = mvc.perform(post("/bookings").header("Authorization", client()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(5, slot(13, 9), 60))).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mvc.perform(get("/bookings/" + id).header("Authorization", "Bearer " + jwt.generate("mallory", "CLIENT")))
                .andExpect(status().isForbidden());
    }
}
