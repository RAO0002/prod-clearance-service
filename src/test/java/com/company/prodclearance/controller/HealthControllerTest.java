package com.company.prodclearance.controller;

import com.company.prodclearance.repository.GroupProcessStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupProcessStatusRepository repository;

    @Test
    void testLiveEndpoint() throws Exception {
        mockMvc.perform(get("/api/health/live"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testReadyEndpoint() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testStatsEndpoint() throws Exception {
        mockMvc.perform(get("/api/health/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timestamp").exists());
    }

}