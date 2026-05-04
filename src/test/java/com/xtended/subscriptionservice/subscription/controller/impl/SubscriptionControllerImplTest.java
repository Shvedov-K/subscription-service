package com.xtended.subscriptionservice.subscription.controller.impl;

import com.xtended.subscriptionservice.subscription.dto.*;
import com.xtended.subscriptionservice.subscription.model.*;
import com.xtended.subscriptionservice.subscription.service.InvoiceService;
import com.xtended.subscriptionservice.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для SubscriptionControllerImpl")
class SubscriptionControllerImplTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionControllerImpl subscriptionController;

    private UUID testUserId;
    private UUID testSubscriptionId;
    private UUID testInvoiceId;
    private User testUser;
    private Subscription testSubscription;
    private Invoice testInvoice;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSubscriptionId = UUID.randomUUID();
        testInvoiceId = UUID.randomUUID();
        testDate = LocalDate.now();
        testUser = new User(testUserId);
        testSubscription = Subscription.builder()
                .id(testSubscriptionId)
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(testDate)
                .active(true)
                .build();

        testInvoice = Invoice.builder()
                .id(testInvoiceId)
                .user(testUser)
                .subscription(testSubscription)
                .issueDate(testDate)
                .amount(BigDecimal.valueOf(100.00))
                .subscriptionType(SubscriptionType.BASIC)
                .subscriptionActivationDate(testDate)
                .build();
    }

    @Test
    @DisplayName("Успешная активация подписки")
    void activate_ValidRequest_ShouldReturnSuccessResponse() {
        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest(
                testUserId, SubscriptionType.BASIC, testDate);

        when(subscriptionService.activateSubscription(testUserId, SubscriptionType.BASIC, testDate))
                .thenReturn(testSubscription);
        doNothing().when(invoiceService).issueInvoice(testSubscription, testDate);

        ResponseEntity<ApiResponse<UUID>> response = subscriptionController.activate(request);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        ApiResponse<UUID> apiResponse = response.getBody();
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("OK", apiResponse.getMessage());
        assertEquals(testUserId, apiResponse.getData());

        verify(subscriptionService, times(1)).activateSubscription(testUserId, SubscriptionType.BASIC, testDate);
        verify(invoiceService, times(1)).issueInvoice(testSubscription, testDate);
    }

    @Test
    @DisplayName("Успешная деактивация подписки")
    void deactivate_ValidRequest_ShouldReturnSuccessResponse() {
        DeactivateSubscriptionRequest request = new DeactivateSubscriptionRequest(
                testUserId, SubscriptionType.BASIC);

        doNothing().when(subscriptionService).deactivateSubscription(testUserId, SubscriptionType.BASIC);

        ResponseEntity<ApiResponse<UUID>> response = subscriptionController.deactivate(request);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        ApiResponse<UUID> apiResponse = response.getBody();
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("OK", apiResponse.getMessage());
        assertEquals(testUserId, apiResponse.getData());

        verify(subscriptionService, times(1)).deactivateSubscription(testUserId, SubscriptionType.BASIC);
    }

    @Test
    @DisplayName("Получение активной подписки пользователя")
    void getActive_ExistingSubscription_ShouldReturnSubscriptionEvent() {
        when(subscriptionService.getActiveSubscription(testUserId))
                .thenReturn(Optional.of(testSubscription));

        ResponseEntity<ApiResponse<SubscriptionEvent>> response = subscriptionController.getActive(testUserId);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        ApiResponse<SubscriptionEvent> apiResponse = response.getBody();
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("OK", apiResponse.getMessage());

        SubscriptionEvent subscriptionEvent = apiResponse.getData();
        assertNotNull(subscriptionEvent);
        assertEquals(testSubscriptionId, subscriptionEvent.getSubscriptionId());
        assertEquals(testUserId, subscriptionEvent.getUserId());
        assertEquals(SubscriptionType.BASIC, subscriptionEvent.getType());
        assertEquals(testDate, subscriptionEvent.getActivationDate());

        verify(subscriptionService, times(1)).getActiveSubscription(testUserId);
    }

    @Test
    @DisplayName("Получение счетов пользователя")
    void getInvoices_ExistingInvoices_ShouldReturnPageOfInvoiceEvents() {
        Pageable pageable = mock(Pageable.class);
        List<Invoice> invoices = List.of(testInvoice);
        Page<Invoice> invoicePage = new PageImpl<>(invoices, pageable, 1);

        when(invoiceService.getUserInvoices(testUserId, pageable)).thenReturn(invoicePage);

        ResponseEntity<ApiResponse<Page<InvoiceEvent>>> response = 
                subscriptionController.getInvoices(testUserId, pageable);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        ApiResponse<Page<InvoiceEvent>> apiResponse = response.getBody();
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("OK", apiResponse.getMessage());

        Page<InvoiceEvent> invoiceEventPage = apiResponse.getData();
        assertNotNull(invoiceEventPage);
        assertEquals(1, invoiceEventPage.getContent().size());

        InvoiceEvent invoiceEvent = invoiceEventPage.getContent().get(0);
        assertEquals(testInvoiceId, invoiceEvent.getInvoiceId());
        assertEquals(testUserId, invoiceEvent.getUserId());
        assertEquals(testDate, invoiceEvent.getInvoiceDate());
        assertEquals(BigDecimal.valueOf(100.00), invoiceEvent.getAmount());
        assertEquals("BASIC", invoiceEvent.getSubscriptionName());
        assertEquals(testDate, invoiceEvent.getSubscriptionActivationDate());

        verify(invoiceService, times(1)).getUserInvoices(testUserId, pageable);
    }

    @Test
    @DisplayName("Получение активной подписки - подписка не найдена")
    void getActive_SubscriptionNotFound_ShouldThrowException() {
        when(subscriptionService.getActiveSubscription(testUserId))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                subscriptionController.getActive(testUserId));

        verify(subscriptionService, times(1)).getActiveSubscription(testUserId);
    }

    @Test
    @DisplayName("Активация подписки с PRO типом")
    void activate_ProSubscriptionType_ShouldWorkCorrectly() {
        ActivateSubscriptionRequest request = new ActivateSubscriptionRequest(
                testUserId, SubscriptionType.PRO, testDate);

        Subscription proSubscription = Subscription.builder()
                .id(testSubscriptionId)
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(testDate)
                .active(true)
                .build();

        when(subscriptionService.activateSubscription(testUserId, SubscriptionType.PRO, testDate))
                .thenReturn(proSubscription);
        doNothing().when(invoiceService).issueInvoice(proSubscription, testDate);

        ResponseEntity<ApiResponse<UUID>> response = subscriptionController.activate(request);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(testUserId, response.getBody().getData());

        verify(subscriptionService, times(1)).activateSubscription(testUserId, SubscriptionType.PRO, testDate);
        verify(invoiceService, times(1)).issueInvoice(proSubscription, testDate);
    }

    @Test
    @DisplayName("Получение пустой страницы счетов")
    void getInvoices_NoInvoices_ShouldReturnEmptyPage() {
        Pageable pageable = mock(Pageable.class);
        Page<Invoice> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(invoiceService.getUserInvoices(testUserId, pageable)).thenReturn(emptyPage);

        ResponseEntity<ApiResponse<Page<InvoiceEvent>>> response = 
                subscriptionController.getInvoices(testUserId, pageable);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        Page<InvoiceEvent> invoiceEventPage = response.getBody().getData();
        assertNotNull(invoiceEventPage);
        assertTrue(invoiceEventPage.getContent().isEmpty());
        assertEquals(0, invoiceEventPage.getTotalElements());

        verify(invoiceService, times(1)).getUserInvoices(testUserId, pageable);
    }
}
