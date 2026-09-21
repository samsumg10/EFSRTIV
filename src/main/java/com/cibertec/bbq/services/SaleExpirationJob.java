package com.cibertec.bbq.services;

import com.cibertec.bbq.repositories.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RN-09: las ventas pendientes con el plazo de pago vencido pasan a EXPIRADO y liberan el cupo.
 * Sustituye al comando payments:verify-payments del sistema Laravel (que consultaba a fincode).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaleExpirationJob {

    private final SaleRepository saleRepository;

    @Scheduled(cron = "0 0 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void expireOverdueSales() {
        int expired = saleRepository.expireOverdue(LocalDate.now(), LocalDateTime.now());
        if (expired > 0) log.info("Ventas expiradas por falta de pago: {}", expired);
    }
}
