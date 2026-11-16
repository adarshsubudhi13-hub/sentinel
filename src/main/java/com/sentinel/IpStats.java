package com.sentinel;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** Mutable per-IP statistics tracked in the registry. */
public final class IpStats {
    public final String ip;
    public final AtomicInteger failures  = new AtomicInteger();
    public final AtomicInteger successes = new AtomicInteger();
    public final AtomicInteger distinct  = new AtomicInteger();
    public volatile boolean blocked     = false;
    public volatile boolean compromised = false;
    public volatile Instant lockedUntil = null;
    public volatile int riskScore       = 0;

    public IpStats(String ip) { this.ip = ip; }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }
}
