package dev.strangevisuals.media;

import dev.strangevisuals.StrangeVisuals;
import dev.strangevisuals.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Fetches album artwork asynchronously for the currently playing track via the iTunes Search API,
 * with procedural gradient texture generation as a visual fallback.
 */
public final class MediaArt {
    private static final Identifier TEXTURE_ID = StrangeVisuals.id("media_art");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static NativeImageBackedTexture texture;
    private static String loadedKey = "";
    private static volatile NativeImage pendingImage;
    private static volatile boolean fetching;
    private static boolean available;

    private MediaArt() {
    }

    public static boolean available() {
        return available;
    }

    public static Identifier textureId() {
        return TEXTURE_ID;
    }

    public static void follow(MediaWatcher.Track track, boolean enabled) {
        if (!enabled || !track.present()) {
            available = false;
            loadedKey = "";
            NativeImage old = pendingImage;
            if (old != null) {
                pendingImage = null;
                old.close();
            }
            return;
        }
        upload();

        String key = track.artist() + " :: " + track.title();
        if (key.equals(loadedKey) || fetching) {
            return;
        }
        loadedKey = key;
        available = false;
        fetching = true;

        String seedKey = key;
        EXECUTOR.submit(() -> {
            NativeImage image = null;
            try {
                byte[] fetched = fetch(track.artist(), track.title());
                if (fetched != null) {
                    image = decode(fetched);
                }
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.debug("Failed to fetch album art for '{} - {}': {}", track.artist(), track.title(), e.getMessage());
            }
            if (image == null) {
                image = paint(seedKey);
            }
            NativeImage prev = pendingImage;
            pendingImage = image;
            if (prev != null && prev != image) {
                prev.close();
            }
            fetching = false;
        });
    }

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "strangevisuals-art");
        thread.setDaemon(true);
        return thread;
    });

    private static void upload() {
        NativeImage image = pendingImage;
        if (image == null) {
            return;
        }
        pendingImage = null;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (texture != null) {
                texture.close();
            }
            texture = new NativeImageBackedTexture(TEXTURE_ID::toString, image);
            client.getTextureManager().registerTexture(TEXTURE_ID, texture);
            texture.upload();
            available = true;
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.warn("Failed to upload album art texture: {}", e.getMessage());
            available = false;
        }
    }

    private static NativeImage decode(byte[] bytes) {
        try {
            return NativeImage.read(bytes);
        } catch (Exception ignored) {
        }
        try {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered == null) {
                return null;
            }
            NativeImage image = new NativeImage(buffered.getWidth(), buffered.getHeight(), false);
            for (int py = 0; py < buffered.getHeight(); py++) {
                for (int px = 0; px < buffered.getWidth(); px++) {
                    image.setColorArgb(px, py, 0xFF000000 | buffered.getRGB(px, py));
                }
            }
            return image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static NativeImage paint(String seed) {
        int size = 96;
        int hash = seed.hashCode();
        float hueA = ((hash >>> 8) & 0xFFFF) / 65535.0f;
        float hueB = (hueA + 0.18f + ((hash >>> 3) & 0x3F) / 512.0f) % 1.0f;

        NativeImage image = new NativeImage(size, size, false);
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                float u = px / (float) (size - 1);
                float v = py / (float) (size - 1);
                float sweep = Math.min(1.0f, Math.max(0.0f, (u + v) * 0.5f));

                float grain = ((px * 73 + py * 151 + hash) & 0x1F) / 31.0f;
                float value = 0.30f + sweep * 0.45f + grain * 0.06f;
                float saturation = 0.55f - sweep * 0.18f;
                int rgb = Theme.hsvToRgb(hueA + (hueB - hueA) * sweep, saturation, value);

                image.setColorArgb(px, py, 0xFF000000 | rgb);
            }
        }
        return image;
    }

    private static byte[] fetch(String artist, String title) throws Exception {
        String term = (artist + " " + title).trim();
        String url = "https://itunes.apple.com/search?media=music&limit=1&term="
            + URLEncoder.encode(term, StandardCharsets.UTF_8);

        HttpResponse<String> meta = CLIENT.send(
            HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        if (meta.statusCode() != 200) {
            return null;
        }

        String artUrl = null;
        try {
            com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(meta.body());
            if (root.isJsonObject()) {
                com.google.gson.JsonArray results = root.getAsJsonObject().getAsJsonArray("results");
                if (results != null && !results.isEmpty()) {
                    com.google.gson.JsonObject first = results.get(0).getAsJsonObject();
                    if (first.has("artworkUrl100") && !first.get("artworkUrl100").isJsonNull()) {
                        artUrl = first.get("artworkUrl100").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.debug("Failed to parse iTunes JSON response: {}", e.getMessage());
        }

        if (artUrl == null) {
            return null;
        }

        artUrl = artUrl.replace("100x100bb", "256x256bb");

        HttpResponse<byte[]> image = CLIENT.send(
            HttpRequest.newBuilder(URI.create(artUrl)).timeout(Duration.ofSeconds(10)).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
        return image.statusCode() == 200 ? image.body() : null;
    }
}
