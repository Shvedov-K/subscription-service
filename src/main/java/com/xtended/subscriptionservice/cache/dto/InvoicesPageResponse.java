package com.xtended.subscriptionservice.cache.dto;

import com.xtended.subscriptionservice.subscription.dto.InvoiceEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для представления страницы с данными о платежах
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicesPageResponse {
    /**
     * Список платежей
     */
    private List<InvoiceEvent> content;
    /**
     * Суммарное количество страниц
     */
    private int totalPages;
    /**
     * Суммарное количество элементов
     */
    private long totalElements;
}
