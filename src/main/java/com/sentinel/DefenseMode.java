package com.sentinel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Autonomous active defense controller: auto-blocks malicious IPs at risk >= 75
 * and synchronizes firewalls with DemoBank via DefenseClient.
 */
public final class DefenseMode {
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicInteger blocksIssued = new AtomicInteger();
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();
    private DefenseClient defenseClient = new DefenseClient();

    public void setDefenseClient(DefenseClient client) {
        this.defenseClient = client;
    }

    public boolean isEnabled() { return enabled.get(); }

    public void setEnabled(boolean on) { enabled.set(on); }

    public void recordBlock(String ip) {
        if (blockedIps.add(ip)) {
            blocksIssued.incrementAndGet();
            if (defenseClient != null) {
                defenseClient.blockIp(ip);
                defenseClient.lockUser("admin", 60);
            }
        }
    }

    public int getBlocksIssued() { return blocksIssued.get(); }

    public Set<String> getBlockedIps() { return Set.copyOf(blockedIps); }

    public void reset() {
        blocksIssued.set(0);
        blockedIps.clear();
        if (defenseClient != null) {
            defenseClient.resetTarget();
        }
    }
}
