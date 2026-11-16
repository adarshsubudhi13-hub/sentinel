package com.sentinel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthService: SHA-256 password hashing with salt + user credential verification.
 * User database loaded from CSV; supports Login Playground for UI demo.
 */
public final class AuthService {
    private static final String ALGORITHM = "SHA-256";
    private final ConcurrentHashMap<String, String[]> users = new ConcurrentHashMap<>(); // user -> [hash, salt]

    public AuthService() { loadDefaults(); }

    /** Load built-in users (username:hash:salt). */
    private void loadDefaults() {
        // alice through eve, password = "password" with static salt for reproducibility
        String[][] seeds = {
            {"alice", hash("password", "s1"), "s1"},
            {"bob",   hash("password", "s2"), "s2"},
            {"carol", hash("password", "s3"), "s3"},
            {"dave",  hash("password", "s4"), "s4"},
            {"eve",   hash("password", "s5"), "s5"}
        };
        for (String[] row : seeds) users.put(row[0], new String[]{row[1], row[2]});
    }

    /** Verify credentials; returns true on match. */
    public boolean verify(String username, String password) {
        String[] record = users.get(username);
        if (record == null) return false;
        return record[0].equals(hash(password, record[1]));
    }

    public boolean userExists(String username) { return users.containsKey(username); }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update((salt + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }
}
