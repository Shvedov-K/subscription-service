package com.xtended.subscriptionservice.subscription.service;

import com.xtended.subscriptionservice.subscription.messaging.OutboxFallbackEventPublisher;
import com.xtended.subscriptionservice.subscription.model.Subscription;
import com.xtended.subscriptionservice.subscription.model.SubscriptionType;
import com.xtended.subscriptionservice.subscription.model.User;
import com.xtended.subscriptionservice.subscription.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для SubscriptionService
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OutboxFallbackEventPublisher eventPublisher;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate FUTURE_DATE = TODAY.plusDays(1);
    private static final LocalDate PAST_DATE = TODAY.minusDays(1);
    private static final SubscriptionType SUBSCRIPTION_TYPE = SubscriptionType.BASIC;

    private User testUser;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testUser = new User(USER_ID);
        testSubscription = Subscription.builder()
                .id(SUBSCRIPTION_ID)
                .user(testUser)
                .type(SUBSCRIPTION_TYPE)
                .activationDate(TODAY)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Должен успешно активировать подписку с корректными параметрами")
    void activateSubscription_SuccessfulActivation_ReturnsSubscription() {
        when(userService.createAndReturnUserById(USER_ID)).thenReturn(testUser);
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        Subscription result = subscriptionService.activateSubscription(USER_ID, SUBSCRIPTION_TYPE, FUTURE_DATE);

        assertNotNull(result);
        assertEquals(testSubscription, result);
        verify(userService).createAndReturnUserById(USER_ID);
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
        verify(subscriptionRepository).save(argThat(subscription ->
                subscription.getUser().equals(testUser) &&
                subscription.getType().equals(SUBSCRIPTION_TYPE) &&
                subscription.getActivationDate().equals(FUTURE_DATE) &&
                subscription.isActive()
        ));
        verify(eventPublisher).publishAfterCommit(eq("subscription.activated.queue"), any(), eq(SUBSCRIPTION_ID.toString()));
    }

    @Test
    @DisplayName("Должен выбросить IllegalArgumentException при активации подписки с прошедшей датой")
    void activateSubscription_PastDate_ThrowsIllegalArgumentException() {
        when(userService.createAndReturnUserById(USER_ID)).thenReturn(testUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                subscriptionService.activateSubscription(USER_ID, SUBSCRIPTION_TYPE, PAST_DATE)
        );

        assertEquals("Activation date must be in the future or today", exception.getMessage());
        verify(userService).createAndReturnUserById(USER_ID);
        verifyNoInteractions(subscriptionRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Должен выбросить IllegalStateException при попытке активации второй подписки")
    void activateSubscription_UserAlreadyHasActiveSubscription_ThrowsIllegalStateException() {
        when(userService.createAndReturnUserById(USER_ID)).thenReturn(testUser);
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(testSubscription));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                subscriptionService.activateSubscription(USER_ID, SUBSCRIPTION_TYPE, FUTURE_DATE)
        );

        assertEquals("User already has an active subscription", exception.getMessage());
        verify(userService).createAndReturnUserById(USER_ID);
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
        verify(subscriptionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Должен успешно активировать подписку с сегодняшней датой")
    void activateSubscription_TodayDate_SuccessfulActivation() {
        when(userService.createAndReturnUserById(USER_ID)).thenReturn(testUser);
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        Subscription result = subscriptionService.activateSubscription(USER_ID, SUBSCRIPTION_TYPE, TODAY);

        assertNotNull(result);
        assertEquals(testSubscription, result);
        verify(userService).createAndReturnUserById(USER_ID);
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(eventPublisher).publishAfterCommit(eq("subscription.activated.queue"), any(), eq(SUBSCRIPTION_ID.toString()));
    }

    @Test
    @DisplayName("Должен успешно деактивировать подписку с корректными параметрами")
    void deactivateSubscription_SuccessfulDeactivation() {
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        assertDoesNotThrow(() -> subscriptionService.deactivateSubscription(USER_ID, SUBSCRIPTION_TYPE));

        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
        verify(subscriptionRepository).save(argThat(subscription ->
                !subscription.isActive() &&
                subscription.getDeactivationDate().equals(TODAY)
        ));
        verify(eventPublisher).publishAfterCommit(eq("subscription.deactivated.queue"), any(), eq(SUBSCRIPTION_ID.toString()));
    }

    @Test
    @DisplayName("Должен выбросить EntityNotFoundException при попытке деактивации несуществующей подписки")
    void deactivateSubscription_NoActiveSubscription_ThrowsEntityNotFoundException() {
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                subscriptionService.deactivateSubscription(USER_ID, SUBSCRIPTION_TYPE)
        );

        assertEquals("Active subscription not found", exception.getMessage());
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
        verify(subscriptionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Должен выбросить EntityNotFoundException при попытке деактивации подписки другого типа")
    void deactivateSubscription_WrongSubscriptionType_ThrowsEntityNotFoundException() {
        Subscription differentTypeSubscription = Subscription.builder()
                .id(SUBSCRIPTION_ID)
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(TODAY)
                .active(true)
                .build();

        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(differentTypeSubscription));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                subscriptionService.deactivateSubscription(USER_ID, SUBSCRIPTION_TYPE)
        );

        assertEquals("Active subscription not found", exception.getMessage());
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
        verify(subscriptionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Должен вернуть активную подписку при её наличии")
    void getActiveSubscription_UserHasActiveSubscription_ReturnsSubscription() {
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(testSubscription));

        Optional<Subscription> result = subscriptionService.getActiveSubscription(USER_ID);

        assertTrue(result.isPresent());
        assertEquals(testSubscription, result.get());
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional при отсутствии активной подписки")
    void getActiveSubscription_NoActiveSubscription_ReturnsEmptyOptional() {
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());

        Optional<Subscription> result = subscriptionService.getActiveSubscription(USER_ID);

        assertFalse(result.isPresent());
        verify(subscriptionRepository).findTopByUserIdAndActiveTrue(USER_ID);
    }

    @Test
    @DisplayName("Должен корректно обработать активацию подписки типа PRO")
    void activateSubscription_ProType_SuccessfulActivation() {
        when(userService.createAndReturnUserById(USER_ID)).thenReturn(testUser);
        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        Subscription result = subscriptionService.activateSubscription(USER_ID, SubscriptionType.PRO, FUTURE_DATE);

        assertNotNull(result);
        assertEquals(testSubscription, result);
        verify(subscriptionRepository).save(argThat(subscription ->
                subscription.getType().equals(SubscriptionType.PRO)
        ));
        verify(eventPublisher).publishAfterCommit(eq("subscription.activated.queue"), any(), eq(SUBSCRIPTION_ID.toString()));
    }

    @Test
    @DisplayName("Должен корректно обработать деактивацию подписки типа PRO")
    void deactivateSubscription_ProType_SuccessfulDeactivation() {
        Subscription proSubscription = Subscription.builder()
                .id(SUBSCRIPTION_ID)
                .user(testUser)
                .type(SubscriptionType.PRO)
                .activationDate(TODAY)
                .active(true)
                .build();

        when(subscriptionRepository.findTopByUserIdAndActiveTrue(USER_ID)).thenReturn(Optional.of(proSubscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(proSubscription);

        assertDoesNotThrow(() -> subscriptionService.deactivateSubscription(USER_ID, SubscriptionType.PRO));

        verify(subscriptionRepository).save(argThat(subscription ->
                !subscription.isActive() &&
                subscription.getDeactivationDate().equals(TODAY)
        ));
        verify(eventPublisher).publishAfterCommit(eq("subscription.deactivated.queue"), any(), eq(SUBSCRIPTION_ID.toString()));
    }
}
