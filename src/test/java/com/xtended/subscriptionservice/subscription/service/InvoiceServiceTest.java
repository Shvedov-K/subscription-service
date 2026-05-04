package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import com.xtended.subscriptionservice.subscription.messaging.OutboxFallbackEventPublisher;
import com.xtended.subscriptionservice.subscription.model.Invoice;
import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import com.xtended.subscriptionservice.subscription.model.User;
import com.xtended.subscriptionservice.subscription.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для InvoiceService")
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private OutboxFallbackEventPublisher eventPublisher;

    @InjectMocks
    private InvoiceService invoiceService;

    private User testUser;
    private Subscription basicSubscription;
    private Subscription proSubscription;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testUser = new User(UUID.randomUUID());
        testDate = LocalDate.now();

        basicSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(testDate.minusMonths(1))
                .active(true)
                .build();

        proSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(testDate.minusMonths(1))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Должен создать счет для подписки типа BASIC")
    void issueInvoice_WhenBasicSubscription_ShouldCreateInvoiceWithCorrectAmount() {
        Invoice savedInvoice = createMockInvoice(basicSubscription, BigDecimal.valueOf(100));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        invoiceService.issueInvoice(basicSubscription, testDate);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());

        Invoice capturedInvoice = invoiceCaptor.getValue();
        assertEquals(testUser, capturedInvoice.getUser());
        assertEquals(basicSubscription, capturedInvoice.getSubscription());
        assertEquals(testDate, capturedInvoice.getIssueDate());
        assertEquals(BigDecimal.valueOf(100), capturedInvoice.getAmount());
        assertEquals(SubscriptionType.BASIC, capturedInvoice.getSubscriptionType());
        assertEquals(basicSubscription.getActivationDate(), capturedInvoice.getSubscriptionActivationDate());

        verify(eventPublisher).publishAfterCommit(eq("invoice.issued.queue"), any(InvoiceEvent.class), eq(savedInvoice.getId().toString()));
        verify(eventPublisher).publishAfterCommit(eq("external.invoice.queue"), any(InvoiceEvent.class), eq(savedInvoice.getId().toString()));
    }

    @Test
    @DisplayName("Должен создать счет для подписки типа PRO")
    void issueInvoice_WhenProSubscription_ShouldCreateInvoiceWithCorrectAmount() {
        Invoice savedInvoice = createMockInvoice(proSubscription, BigDecimal.valueOf(200));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        invoiceService.issueInvoice(proSubscription, testDate);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());

        Invoice capturedInvoice = invoiceCaptor.getValue();
        assertEquals(testUser, capturedInvoice.getUser());
        assertEquals(proSubscription, capturedInvoice.getSubscription());
        assertEquals(testDate, capturedInvoice.getIssueDate());
        assertEquals(BigDecimal.valueOf(200), capturedInvoice.getAmount());
        assertEquals(SubscriptionType.PRO, capturedInvoice.getSubscriptionType());
        assertEquals(proSubscription.getActivationDate(), capturedInvoice.getSubscriptionActivationDate());

        verify(eventPublisher).publishAfterCommit(eq("invoice.issued.queue"), any(InvoiceEvent.class), eq(savedInvoice.getId().toString()));
        verify(eventPublisher).publishAfterCommit(eq("external.invoice.queue"), any(InvoiceEvent.class), eq(savedInvoice.getId().toString()));
    }

    @Test
    @DisplayName("Должен получить счета пользователя с пагинацией")
    void getUserInvoices_WhenValidUserId_ShouldReturnUserInvoices() {
        Pageable pageable = mock(Pageable.class);
        List<Invoice> invoices = List.of(
                createMockInvoice(basicSubscription, BigDecimal.valueOf(100)),
                createMockInvoice(proSubscription, BigDecimal.valueOf(200))
        );
        Page<Invoice> expectedPage = new PageImpl<>(invoices);

        when(invoiceRepository.findByUserIdOrderByIssueDateDesc(testUser.getId(), pageable))
                .thenReturn(expectedPage);

        Page<Invoice> result = invoiceService.getUserInvoices(testUser.getId(), pageable);

        assertEquals(expectedPage, result);
        verify(invoiceRepository).findByUserIdOrderByIssueDateDesc(testUser.getId(), pageable);
    }

    @Test
    @DisplayName("Должен вернуть пустую страницу когда у пользователя нет счетов")
    void getUserInvoices_WhenUserHasNoInvoices_ShouldReturnEmptyPage() {
        Pageable pageable = mock(Pageable.class);
        Page<Invoice> emptyPage = new PageImpl<>(List.of());

        when(invoiceRepository.findByUserIdOrderByIssueDateDesc(testUser.getId(), pageable))
                .thenReturn(emptyPage);

        Page<Invoice> result = invoiceService.getUserInvoices(testUser.getId(), pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(invoiceRepository).findByUserIdOrderByIssueDateDesc(testUser.getId(), pageable);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать разные даты выставления счета")
    void issueInvoice_WhenDifferentIssueDate_ShouldUseProvidedDate() {
        LocalDate customDate = LocalDate.of(2024, 6, 15);
        Invoice savedInvoice = createMockInvoice(basicSubscription, BigDecimal.valueOf(100));
        savedInvoice.setIssueDate(customDate);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        invoiceService.issueInvoice(basicSubscription, customDate);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());

        Invoice capturedInvoice = invoiceCaptor.getValue();
        assertEquals(customDate, capturedInvoice.getIssueDate());
    }

    @Test
    @DisplayName("Должен отправить события в обе очереди после создания счета")
    void issueInvoice_WhenInvoiceCreated_ShouldPublishEventsToBothQueues() {
        Invoice savedInvoice = createMockInvoice(proSubscription, BigDecimal.valueOf(200));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        invoiceService.issueInvoice(proSubscription, testDate);

        ArgumentCaptor<InvoiceEvent> eventCaptor = ArgumentCaptor.forClass(InvoiceEvent.class);
        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);


        verify(eventPublisher).publishAfterCommit(eq("invoice.issued.queue"), eventCaptor.capture(), eventIdCaptor.capture());
        verify(eventPublisher).publishAfterCommit(eq("external.invoice.queue"), eventCaptor.capture(), eventIdCaptor.capture());

        InvoiceEvent capturedEvent = eventCaptor.getValue();
        String capturedEventId = eventIdCaptor.getValue();

        assertEquals(savedInvoice.getId().toString(), capturedEventId);
        assertEquals(savedInvoice.getId(), capturedEvent.getInvoiceId());
        assertEquals(testUser.getId(), capturedEvent.getUserId());
        assertEquals(testDate, capturedEvent.getInvoiceDate());
        assertEquals(BigDecimal.valueOf(200), capturedEvent.getAmount());
        assertEquals("PRO", capturedEvent.getSubscriptionName());
        assertEquals(proSubscription.getActivationDate(), capturedEvent.getSubscriptionActivationDate());
    }

    @Test
    @DisplayName("Должен выбросить исключение когда подписка равна null")
    void issueInvoice_WhenSubscriptionIsNull_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> invoiceService.issueInvoice(null, testDate));

        verify(invoiceRepository, never()).save(any());
        verify(eventPublisher, never()).publishAfterCommit(any(), any(), any());
    }

    @Test
    @DisplayName("Должен выбросить исключение когда дата выставления счета равна null")
    void issueInvoice_WhenIssueDateIsNull_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> invoiceService.issueInvoice(basicSubscription, null));

        verify(invoiceRepository, never()).save(any());
        verify(eventPublisher, never()).publishAfterCommit(any(), any(), any());
    }

    private Invoice createMockInvoice(Subscription subscription, BigDecimal amount) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .user(subscription.getUser())
                .subscription(subscription)
                .issueDate(testDate)
                .amount(amount)
                .subscriptionType(subscription.getType())
                .subscriptionActivationDate(subscription.getActivationDate())
                .build();
    }
}
