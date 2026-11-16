package dev.strangevisuals.media;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asynchronously fetches and parses synchronized (LRC) and plain lyrics
 * for currently playing tracks via the lrclib.net public API.
 */
public final class Lyrics {
    private static final Pattern LINE = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]\\s*(.*)");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public record Line(long startMillis, String text) {
    }

    private static volatile List<Line> lines = List.of();
    private static volatile String loadedKey = "";
    private static volatile boolean fetching;

    private Lyrics() {
    }

    public static List<Line> lines() {
        return lines;
    }

    public static void follow(MediaWatcher.Track track) {
        if (!track.present()) {
            lines = List.of();
            loadedKey = "";
            return;
        }
        String key = track.artist() + " :: " + track.title();
        if (key.equals(loadedKey) || fetching) {
            return;
        }
        loadedKey = key;
        lines = List.of();
        fetching = true;

        EXECUTOR.submit(() -> {
            try {
                lines = fetch(track.artist(), track.title(), track.length());
            } catch (Exception e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.debug("Failed to fetch lyrics for '{} - {}': {}", track.artist(), track.title(), e.getMessage());
                lines = List.of();
            } finally {
                fetching = false;
            }
        });
    }

    private static final java.util.concurrent.ExecutorService EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "strangevisuals-lyrics");
        thread.setDaemon(true);
        return thread;
    });

    public static Line at(long positionMillis, long holdMillis) {
        List<Line> current = lines;
        if (current.isEmpty()) {
            return null;
        }
        Line best = null;
        for (Line line : current) {
            if (line.startMillis() > positionMillis) {
                break;
            }
            best = line;
        }
        if (best == null || positionMillis - best.startMillis() > holdMillis) {
            return null;
        }
        return best;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static List<Line> spread(String plain, long duration) {
        if (plain == null || plain.isBlank() || duration <= 0L) {
            return List.of();
        }
        List<String> rows = new ArrayList<>();
        for (String raw : plain.split("\n")) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                rows.add(trimmed);
            }
        }
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Line> paced = new ArrayList<>(rows.size());
        long step = duration / rows.size();
        for (int i = 0; i < rows.size(); i++) {
            paced.add(new Line(i * step, rows.get(i)));
        }
        return List.copyOf(paced);
    }

    private static List<Line> fetch(String artist, String title, long duration) throws Exception {
        String url = "https://lrclib.net/api/get?artist_name=" + encode(artist) + "&track_name=" + encode(title);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "StrangeVisuals (Minecraft mod)")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return List.of();
        }

        try {
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) {
                return List.of();
            }
            com.google.gson.JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
                List<Line> synced = parse(obj.get("syncedLyrics").getAsString());
                if (!synced.isEmpty()) {
                    return synced;
                }
            }
            if (obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull()) {
                return spread(obj.get("plainLyrics").getAsString(), duration);
            }
        } catch (Exception e) {
            dev.strangevisuals.StrangeVisuals.LOGGER.debug("Failed to parse lyrics JSON: {}", e.getMessage());
        }

        return List.of();
    }

    private static List<Line> parse(String lrc) {
        if (lrc == null || lrc.isBlank()) {
            return List.of();
        }
        List<Line> parsed = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher matcher = LINE.matcher(raw.trim());
            if (!matcher.matches()) {
                continue;
            }
            String text = matcher.group(4).trim();
            if (text.isEmpty()) {
                continue;
            }
            long millis = Long.parseLong(matcher.group(1)) * 60_000L + Long.parseLong(matcher.group(2)) * 1000L;
            String fraction = matcher.group(3);
            if (fraction != null) {
                millis += Long.parseLong(fraction) * (fraction.length() == 2 ? 10L : 1L);
            }
            parsed.add(new Line(millis, text));
        }
        Collections.sort(parsed, (a, b) -> Long.compare(a.startMillis(), b.startMillis()));
        return List.copyOf(parsed);
    }
}
