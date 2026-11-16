package com.sentinel;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    @Test
    void testConcurrentSubmissions() throws InterruptedException {
        Scorecard scorecard = new Scorecard();
        DefenseMode defense = new DefenseMode();
        InMemoryStorageService storage = new InMemoryStorageService();
        DetectionEngine engine = new DetectionEngine(Rules.buildAll(), storage, defense, scorecard);
        engine.start();

        int threadCount = 10;
        int eventsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        LoginEvent ev = new LoginEvent(
                            "id-" + threadId + "-" + j,
                            Instant.now(),
                            "10.0." + (threadId % 5) + ".1",
                            "user" + (j % 5),
                            j % 10 == 0
                        );
                        engine.submit(ev);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Wait a short moment for background consumer thread to drain queue
        Thread.sleep(1500);

        assertTrue(engine.getTotalAttempts() > 0);
        engine.stop();
    }
}
