package com.notificationapp.integration.service;

import com.notificationapp.service.DeliveryProcessor;
import com.notificationapp.service.EmailNotificationSender;
import com.notificationapp.service.OutboxProcessor;
import com.notificationapp.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DeliveryConcurrencyIT extends AbstractIntegrationTest {

    private static final int EVENTS = 40;
    private static final int WORKERS = 4;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private DeliveryProcessor deliveryProcessor;

    @MockitoSpyBean
    private EmailNotificationSender emailSender;

    @Test
    void shouldSendEachDeliveryExactlyOnceWithConcurrentWorkers() throws Exception {
        String jane = createUser("jane.doe@example.com", "Jane Doe");
        createAlert(jane, "Quakes", "natural_disaster");
        for (int i = 0; i < EVENTS; i++) {
            createEvent("natural_disaster", "Earthquake " + i);
        }
        while (outboxProcessor.processNext()) {
            // turn every event into its delivery job
        }

        List<Integer> sentPerWorker = runWorkersConcurrently();

        assertThat(sentPerWorker.stream().mapToInt(Integer::intValue).sum()).isEqualTo(EVENTS);
        verify(emailSender, times(EVENTS)).send(any());
        Integer sent = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_deliveries WHERE status = 'SENT'", Integer.class);
        assertThat(sent).isEqualTo(EVENTS);
    }

    /** Each worker sends jobs until none is left, and reports how many it handled. */
    private List<Integer> runWorkersConcurrently() throws Exception {
        Callable<Integer> worker = () -> {
            int handled = 0;
            while (deliveryProcessor.processNext()) {
                handled++;
            }
            return handled;
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
