package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import com.xtended.subscriptionservice.subscription.model.User;
import com.xtended.subscriptionservice.subscription.repository.InvoiceRepository;
import com.xtended.subscriptionservice.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для InvoiceScheduler")
class InvoiceSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceScheduler invoiceScheduler;

    private User testUser;
    private Subscription inactiveSubscription;
    private Subscription futureSubscription;

    @BeforeEach
    void setUp() {
        testUser = new User(UUID.randomUUID());

        LocalDate today = LocalDate.now();
        LocalDate activationDate = today.withDayOfMonth(15);

        inactiveSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(activationDate)
                .active(false)
                .build();

        futureSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(today.plusMonths(1))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Должен выставить счета для активных подписок в нужную дату")
    void issueMonthlyInvoices_WhenActiveSubscriptionAndMatchingDate_ShouldIssueInvoice() {
        LocalDate today = LocalDate.now();
        LocalDate matchingActivationDate = today.withDayOfMonth(today.getDayOfMonth());

        Subscription matchingSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(matchingActivationDate)
                .active(true)
                .build();

        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(List.of(matchingSubscription));
        when(invoiceRepository.existsBySubscriptionAndIssueDate(matchingSubscription, today))
                .thenReturn(false);

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService).issueInvoice(matchingSubscription, today);
        verify(invoiceRepository).existsBySubscriptionAndIssueDate(matchingSubscription, today);
    }

    @Test
    @DisplayName("Не должен выставлять счет если он уже существует за эту дату")
    void issueMonthlyInvoices_WhenInvoiceAlreadyExists_ShouldNotIssueInvoice() {
        LocalDate today = LocalDate.now();
        LocalDate activationDate = today.withDayOfMonth(today.getDayOfMonth());

        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(activationDate)
                .active(true)
                .build();

        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(List.of(subscription));
        when(invoiceRepository.existsBySubscriptionAndIssueDate(subscription, today))
                .thenReturn(true);

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService, never()).issueInvoice(any(), any());
        verify(invoiceRepository).existsBySubscriptionAndIssueDate(subscription, today);
    }

    @Test
    @DisplayName("Не должен выставлять счет для неактивных подписок")
    void issueMonthlyInvoices_WhenSubscriptionInactive_ShouldNotIssueInvoice() {
        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(List.of(inactiveSubscription));

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService, never()).issueInvoice(any(), any());
        verify(invoiceRepository, never()).existsBySubscriptionAndIssueDate(any(), any());
    }

    @Test
    @DisplayName("Не должен выставлять счет если дата активации в будущем")
    void issueMonthlyInvoices_WhenActivationDateInFuture_ShouldNotIssueInvoice() {
        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(List.of(futureSubscription));

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService, never()).issueInvoice(any(), any());
        verify(invoiceRepository, never()).existsBySubscriptionAndIssueDate(any(), any());
    }

    @Test
    @DisplayName("Не должен выставлять счет если день месяца не совпадает")
    void issueMonthlyInvoices_WhenDayOfMonthNotMatching_ShouldNotIssueInvoice() {
        LocalDate today = LocalDate.now();
        LocalDate differentActivationDate = today.withDayOfMonth(
                today.getDayOfMonth() == 1 ? 2 : 1
        );

        Subscription nonMatchingSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(differentActivationDate)
                .active(true)
                .build();

        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(List.of(nonMatchingSubscription));

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService, never()).issueInvoice(any(), any());
        verify(invoiceRepository, never()).existsBySubscriptionAndIssueDate(any(), any());
    }

    @Test
    @DisplayName("Должен обработать несколько подписок корректно")
    void issueMonthlyInvoices_WhenMultipleSubscriptions_ShouldProcessCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate matchingActivationDate = today.withDayOfMonth(today.getDayOfMonth());

        Subscription matchingSubscription1 = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(matchingActivationDate)
                .active(true)
                .build();

        Subscription matchingSubscription2 = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(matchingActivationDate)
                .active(true)
                .build();

        Subscription nonMatchingSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .type(SubscriptionType.BASIC)
                .activationDate(today.withDayOfMonth(today.getDayOfMonth() == 1 ? 2 : 1))
                .active(true)
                .build();

        List<Subscription> subscriptions = List.of(
                matchingSubscription1,
                matchingSubscription2,
                nonMatchingSubscription,
                inactiveSubscription
        );

        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(subscriptions);
        when(invoiceRepository.existsBySubscriptionAndIssueDate(matchingSubscription1, today))
                .thenReturn(false);
        when(invoiceRepository.existsBySubscriptionAndIssueDate(matchingSubscription2, today))
                .thenReturn(true);

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService).issueInvoice(matchingSubscription1, today);
        verify(invoiceService, never()).issueInvoice(matchingSubscription2, today);
        verify(invoiceService, never()).issueInvoice(nonMatchingSubscription, today);
        verify(invoiceService, never()).issueInvoice(inactiveSubscription, today);
    }

    @Test
    @DisplayName("Должен обработать пустой список подписок")
    void issueMonthlyInvoices_WhenNoActiveSubscriptions_ShouldNotIssueAnyInvoices() {
        when(subscriptionRepository.findAllByActiveTrue())
                .thenReturn(List.of());

        invoiceScheduler.issueMonthlyInvoices();

        verify(invoiceService, never()).issueInvoice(any(), any());
        verify(invoiceRepository, never()).existsBySubscriptionAndIssueDate(any(), any());
    }
}
