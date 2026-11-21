package com.notificationapp.integration.service;

import com.notificationapp.service.OutboxProcessor;
import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxConcurrencyIT extends AbstractIntegrationTest {

    private static final int EVENTS = 40;
    private static final int WORKERS = 4;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Test
    void shouldProcessEachRecordExactlyOnceWithConcurrentWorkers() throws Exception {
        String jane = createUser("jane.doe@example.com", "Jane Doe");
        createAlert(jane, "Quakes", "natural_disaster");
        for (int i = 0; i < EVENTS; i++) {
            createEvent("natural_disaster", "Earthquake " + i);
        }

        List<Integer> processedPerWorker = runWorkersConcurrently();

        assertThat(processedPerWorker.stream().mapToInt(Integer::intValue).sum()).isEqualTo(EVENTS);
        Integer processedRecords = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_outbox WHERE status = 'PROCESSED'", Integer.class);
        Integer deliveries = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_deliveries", Integer.class);
        assertThat(processedRecords).isEqualTo(EVENTS);
        assertThat(deliveries).isEqualTo(EVENTS);
    }

    /** Each worker processes records until none is left, and reports how many it processed. */
    private List<Integer> runWorkersConcurrently() throws Exception {
        Callable<Integer> worker = () -> {
            int processed = 0;
            while (outboxProcessor.processNext()) {
                processed++;
            }
            return processed;
        };
        try (ExecutorService executor = Executors.newFixedThreadPool(WORKERS)) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < WORKERS; i++) {
                futures.add(executor.submit(worker));
            }
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }

}
