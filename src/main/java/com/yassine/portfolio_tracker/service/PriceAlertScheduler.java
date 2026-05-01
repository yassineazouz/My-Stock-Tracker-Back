package com.yassine.portfolio_tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PriceAlertScheduler {

    private final PriceAlertService priceAlertService;

    @Value("${alerts.check.enabled:true}")
    private boolean enabled;

    public PriceAlertScheduler(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    @Scheduled(fixedDelayString = "${alerts.check.fixed-delay-ms:60000}")
    public void checkActiveAlerts() {
        if (!enabled) {
            return;
        }

        int triggeredCount = priceAlertService.checkActiveAlerts().size();
        if (triggeredCount > 0) {
            log.info("Triggered {} price alert(s)", triggeredCount);
        }
    }
}
