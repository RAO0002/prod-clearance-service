package com.company.prodclearance.service;

import com.company.prodclearance.model.GroupProcessStatus;
import com.company.prodclearance.repository.GroupProcessStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearanceProcessingServiceTest {

    @Mock
    private GroupProcessStatusRepository repository;

    private ClearanceProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ClearanceProcessingService(repository);
    }

    @Test
    void testExecuteClearanceLogicSuccess() {
        // Arrange
        GroupProcessStatus record = GroupProcessStatus.builder()
            .id("123")
            .type("PROD")
            .productId("PROD-001")
            .status("R")
            .updatedDt(LocalDateTime.now())
            .awsRequestId("aws-req-001")
            .build();

        when(repository.save(any())).thenReturn(record);

        // Act
        boolean result = service.executeClearanceLogic(record);

        // Assert
        assertTrue(result);
        assertEquals("C", record.getStatus());
        verify(repository, times(1)).save(any());
    }

    @Test
    void testExecuteClearanceLogicWithNullProductId() {
        // Arrange
        GroupProcessStatus record = GroupProcessStatus.builder()
            .id("123")
            .type("PROD")
            .productId(null)
            .status("R")
            .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            service.executeClearanceLogic(record)
        );
    }

}