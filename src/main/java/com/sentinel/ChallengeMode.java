package com.sentinel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Challenge Mode: "Stop attack before 3 accounts compromised."
 * Tracks user manual IP blocks under time pressure, compromises, and computes score.
 */
public final class ChallengeMode {
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicInteger compromises = new AtomicInteger();
    private final AtomicInteger manualBlocks = new AtomicInteger();
    private final AtomicInteger correctBlocks = new AtomicInteger();
    private final AtomicLong startMs = new AtomicLong();
    private final AtomicLong endMs = new AtomicLong();

    public void start() {
        active.set(true);
        compromises.set(0);
        manualBlocks.set(0);
        correctBlocks.set(0);
        startMs.set(System.currentTimeMillis());
        endMs.set(0);
    }

    public void stop() {
        if (active.compareAndSet(true, false)) {
            endMs.set(System.currentTimeMillis());
        }
    }

    public boolean isActive() {
        return active.get();
    }

    public void recordCompromise() {
        if (!active.get()) return;
        int current = compromises.incrementAndGet();
        if (current >= Defaults.CHALLENGE_MAX_COMPROMISES) {
            stop();
        }
    }

    public void recordManualBlock(boolean wasAttacker) {
        if (!active.get()) return;
        manualBlocks.incrementAndGet();
        if (wasAttacker) {
            correctBlocks.incrementAndGet();
        }
    }

    public int getCompromises() { return compromises.get(); }
    public int getManualBlocks() { return manualBlocks.get(); }
    public int getCorrectBlocks() { return correctBlocks.get(); }

    public long getDurationSeconds() {
        long finish = endMs.get() > 0 ? endMs.get() : System.currentTimeMillis();
        return Math.max(0, (finish - startMs.get()) / 1000);
    }

    public boolean isWon() {
        return compromises.get() < Defaults.CHALLENGE_MAX_COMPROMISES && !active.get() && startMs.get() > 0;
    }

    public int calculateScore() {
        if (compromises.get() >= Defaults.CHALLENGE_MAX_COMPROMISES) return 0;
        int accuracyScore = manualBlocks.get() == 0 ? 0 : (int) (((double) correctBlocks.get() / manualBlocks.get()) * 50);
        int penalty = (int) (getDurationSeconds() * 0.5);
        int compromisePenalty = compromises.get() * 20;
        return Math.max(0, 100 + accuracyScore - penalty - compromisePenalty);
    }
}
