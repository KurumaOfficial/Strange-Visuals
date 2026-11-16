package dev.strangevisuals.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.strangevisuals.StrangeVisuals;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.StringHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AccountStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("strangevisuals_accounts.json");
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private static final List<StoredAccount> accounts = new ArrayList<>();

    private AccountStorage() {
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        String trimmed = username.trim();
        return trimmed.length() >= 3 && trimmed.length() <= 16 && StringHelper.isValidPlayerName(trimmed);
    }

    public static void load() {
        accounts.clear();
        if (!Files.isRegularFile(FILE)) {
            return;
        }
        boolean pruned = false;
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            List<String> names = GSON.fromJson(reader, LIST_TYPE);
            if (names != null) {
                for (String name : names) {
                    if (isValidUsername(name)) {
                        accounts.add(new StoredAccount(name.trim()));
                    } else {
                        pruned = true;
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            StrangeVisuals.LOGGER.error("Failed to load accounts, starting empty: {}", e.getMessage());
        }
        if (pruned) {
            save();
        }
    }

    public static void save() {
        List<String> names = new ArrayList<>();
        for (StoredAccount account : accounts) {
            if (isValidUsername(account.username())) {
                names.add(account.username());
            }
        }
        Path parent = FILE.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                StrangeVisuals.LOGGER.error("Failed to create directories for accounts: {}", e.getMessage());
                return;
            }
        }
        Path tempFile = FILE.resolveSibling(FILE.getFileName().toString() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(names, LIST_TYPE, writer);
            }
            try {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempFile, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            StrangeVisuals.LOGGER.error("Failed to save accounts: {}", e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
        }
    }

    public static List<StoredAccount> accounts() {
        return accounts;
    }

    public static boolean add(String username) {
        if (!isValidUsername(username)) {
            return false;
        }
        String clean = username.trim();
        for (StoredAccount account : accounts) {
            if (account.username().equalsIgnoreCase(clean)) {
                return false;
            }
        }
        accounts.add(new StoredAccount(clean));
        save();
        return true;
    }

    public static void remove(StoredAccount account) {
        accounts.remove(account);
        save();
    }
}
