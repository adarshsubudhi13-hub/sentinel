package com.sentinel;

/** Receives fired alerts for side effects (logging, SSE, console). */
public interface AlertListener {
    void onAlert(Alert alert);
}
