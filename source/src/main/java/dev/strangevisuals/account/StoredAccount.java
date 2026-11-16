package dev.strangevisuals.account;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class StoredAccount {
    private final String username;

    public StoredAccount(String username) {
        this.username = username;
    }

    public String username() {
        return username;
    }

    public UUID uuid() {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
