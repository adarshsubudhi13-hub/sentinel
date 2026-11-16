package com.sentinel;

import java.util.UUID;

/** ID generator utility. */
public final class IdGen {
    private IdGen() {}

    public static String next() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
