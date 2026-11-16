package dev.strangevisuals.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.strangevisuals.StrangeVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * High-performance, zero-allocation translation engine for StrangeVisuals.
 * Supports auto-detection of Minecraft language with native Russian, Ukrainian, and English translations.
 */
public final class I18n {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();
    private static String languageOverride = "auto";

    static {
        loadLanguage("en_us");
        loadLanguage("ru_ru");
        loadLanguage("uk_ua");
    }

    private I18n() {
    }

    private static void loadLanguage(String langCode) {
        String path = "/assets/strangevisuals/lang/" + langCode + ".json";
        try (java.io.InputStream stream = I18n.class.getResourceAsStream(path)) {
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
                    if (map != null) {
                        TRANSLATIONS.put(langCode, map);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            StrangeVisuals.LOGGER.warn("Failed to load embedded translation bundle for '{}': {}", langCode, e.getMessage());
        }

        Identifier id = StrangeVisuals.id("lang/" + langCode + ".json");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return;
        }
        Optional<Resource> resource = client.getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8)) {
            Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
            if (map != null) {
                TRANSLATIONS.put(langCode, map);
            }
        } catch (Exception e) {
            StrangeVisuals.LOGGER.warn("Failed to load resource translation bundle for '{}': {}", langCode, e.getMessage());
        }
    }

    private static volatile String cachedEffectiveLanguage = null;
    private static String lastGameLang = null;

    public static void setLanguageOverride(String lang) {
        languageOverride = lang != null ? lang.toLowerCase() : "auto";
        cachedEffectiveLanguage = null;
    }

    public static String getLanguageOverride() {
        return languageOverride;
    }

    public static String getEffectiveLanguage() {
        if (!"auto".equalsIgnoreCase(languageOverride) && TRANSLATIONS.containsKey(languageOverride)) {
            return languageOverride;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getLanguageManager() != null) {
            String gameLang = client.getLanguageManager().getLanguage();
            if (gameLang != null && gameLang.equals(lastGameLang) && cachedEffectiveLanguage != null) {
                return cachedEffectiveLanguage;
            }
            lastGameLang = gameLang;
            if (gameLang != null) {
                String lower = gameLang.toLowerCase();
                if (lower.startsWith("ru")) return cachedEffectiveLanguage = "ru_ru";
                if (lower.startsWith("uk")) return cachedEffectiveLanguage = "uk_ua";
            }
        }
        return cachedEffectiveLanguage = "en_us";
    }

    public static String get(String key) {
        if (key == null) return "";
        if (TRANSLATIONS.isEmpty()) {
            loadLanguage("en_us");
            loadLanguage("ru_ru");
            loadLanguage("uk_ua");
        }
        String lang = getEffectiveLanguage();
        Map<String, String> bundle = TRANSLATIONS.get(lang);
        if (bundle != null) {
            String val = bundle.get(key);
            if (val != null) return val;
        }
        // Fallback to English
        Map<String, String> en = TRANSLATIONS.get("en_us");
        if (en != null) {
            String val = en.get(key);
            if (val != null) return val;
        }
        // Fallback to Minecraft vanilla translation
        if (net.minecraft.client.resource.language.I18n.hasTranslation(key)) {
            return net.minecraft.client.resource.language.I18n.translate(key);
        }
        return key;
    }

    public static String get(String key, Object... args) {
        String base = get(key);
        if (args != null && args.length > 0) {
            try {
                return String.format(base, args);
            } catch (Exception e) {
                StrangeVisuals.LOGGER.warn("Failed to format translation string for key '{}': {}", key, e.getMessage());
            }
        }
        return base;
    }
}
