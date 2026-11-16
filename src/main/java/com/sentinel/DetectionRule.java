package com.sentinel;

/** Functional interface: a single detection rule applied to a window snapshot. */
@FunctionalInterface
public interface DetectionRule {
    /**
     * Evaluate a login event in context.
     * @return an Alert if the rule fires, or null.
     */
    Alert evaluate(LoginEvent event, IpStats stats, SlidingWindow window);

    default String ruleId() { return "UNKNOWN"; }
}
