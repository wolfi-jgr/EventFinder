package com.example.eventfinder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(EventCleanupScheduler.class);

    private final EventService eventService;

    public EventCleanupScheduler(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(
        cron = "${app.events.cleanup.cron:0 15 6 * * *}",
        zone = "${app.events.cleanup.zone:Europe/Vienna}"
    )
    public void cleanupExpiredEvents() {
        int deleted = eventService.deleteExpiredEvents();
        if (deleted > 0) {
            log.info("Deleted {} expired events", deleted);
        }
    }
}
