package com.sentinel;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detection Scorecard: tracks per-run metrics and produces an A-F grade.
 * Reset at the start of each simulation run.
 */
public final class Scorecard {
    private final AtomicInteger attacksLaunched   = new AtomicInteger();
    private final AtomicInteger attacksDetected   = new AtomicInteger();
    private final AtomicInteger falsePositives     = new AtomicInteger();
    private final AtomicInteger compromises        = new AtomicInteger();
    private final AtomicLong firstAlertMs          = new AtomicLong(0);
    private final AtomicReference<Instant> startTs = new AtomicReference<>();

    public void startRun(int attackCount) {
        startTs.set(Instant.now());
        attacksLaunched.set(attackCount);
        attacksDetected.set(0);
        falsePositives.set(0);
        compromises.set(0);
        firstAlertMs.set(0);
    }

    public void recordAlert(Alert a) {
        attacksDetected.incrementAndGet();
        firstAlertMs.compareAndSet(0,
            startTs.get() == null ? 0 :
            Instant.now().toEpochMilli() - startTs.get().toEpochMilli());
        if ("NORMAL".equals(a.ruleName())) falsePositives.incrementAndGet();
        if ("CRITICAL".equals(a.severity())) compromises.incrementAndGet();
    }

    public void reset() {
        attacksLaunched.set(0); attacksDetected.set(0);
        falsePositives.set(0); compromises.set(0); firstAlertMs.set(0);
        startTs.set(null);
    }

    public int getAttacksLaunched() { return attacksLaunched.get(); }
    public int getAttacksDetected() { return attacksDetected.get(); }
    public int getFalsePositives()  { return falsePositives.get(); }
    public int getCompromises()     { return compromises.get(); }
    public long getTimeToDetectMs() { return firstAlertMs.get(); }

    public String grade() {
        int launched = attacksLaunched.get();
        if (launched == 0) return "N/A";
        double rate = (double) attacksDetected.get() / launched;
        int fp = falsePositives.get();
        if (rate >= 0.9 && fp == 0) return "A";
        if (rate >= 0.75 && fp <= 1) return "B";
        if (rate >= 0.6)  return "C";
        if (rate >= 0.4)  return "D";
        return "F";
    }
}
