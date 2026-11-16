package dev.strangevisuals.media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Windows Media Transport Integration service.
 * <p>
 * Spawns an isolated PowerShell sub-process querying {@code Windows.Media.Control}
 * via Windows Runtime (WinRT) APIs to stream playback track title, artist, app ID,
 * and playback timestamps asynchronously through stdout without temp script files.
 */
public final class MediaWatcher {
    private static final String SCRIPT = """
        [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
        $OutputEncoding = [System.Text.Encoding]::UTF8
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
            $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
            $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
        function Await($op, $type) {
            $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
            $task.Wait(2500) | Out-Null
            $task.Result
        }
        $mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
        $mgr = Await ($mgrType::RequestAsync()) ($mgrType)
        $propType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]
        while ($true) {
            $best = ''
            $bestScore = -1000
            try {
                $sessions = $mgr.GetSessions()
                foreach ($s in $sessions) {
                    $props = Await ($s.TryGetMediaPropertiesAsync()) ($propType)
                    if ($null -eq $props) { continue }
                    $title = ("" + $props.Title).Trim()
                    $artist = ("" + $props.Artist).Trim()
                    if ([string]::IsNullOrWhiteSpace($artist)) { $artist = ("" + $props.AlbumArtist).Trim() }
                    if ([string]::IsNullOrWhiteSpace($title) -and [string]::IsNullOrWhiteSpace($artist)) { continue }

                    $info = $s.GetPlaybackInfo()
                    $playing = '0'
                    if ("$($info.PlaybackStatus)" -eq 'Playing') { $playing = '1' }

                    $pos = 0
                    $len = 0
                    try {
                        $tl = $s.GetTimelineProperties()
                        $pos = [math]::Round($tl.Position.TotalMilliseconds)
                        $len = [math]::Round($tl.EndTime.TotalMilliseconds)
                    } catch { }

                    $app = "" + $s.SourceAppUserModelId
                    $line = "$title`t$artist`t$app`t$playing`t$pos`t$len"
                    $score = 0
                    if ($playing -eq '1') { $score += 50 }
                    if ("$($props.PlaybackType)" -eq 'Music') { $score += 20 }
                    $id = $app.ToLower()
                    if ($id -match 'spotify|soundcloud|yandex|music|deezer|tidal|aimp|foobar|winamp|apple') { $score += 100 }
                    elseif ($id -match 'chrome|edge|firefox|opera|brave') { $score += 10 }
                    if ($score -gt $bestScore) { $bestScore = $score; $best = $line }
                }
            } catch { }
            Write-Output $best
            [Console]::Out.Flush()
            Start-Sleep -Milliseconds 1500
        }
        """;

    private static volatile Track track = Track.NONE;
    private static Process process;
    private static boolean started;

    private MediaWatcher() {
    }

    public record Track(String title, String artist, String app, boolean playing,
                        long position, long length) {
        public static final Track NONE = new Track("", "", "", false, 0L, 0L);

        public boolean present() {
            return !title.isBlank();
        }
    }

    public static Track current() {
        return track;
    }

    public static synchronized void ensureRunning() {
        if (started) {
            return;
        }
        started = true;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(MediaWatcher::stop, "strangevisuals-media-cleanup"));
        Thread thread = new Thread(MediaWatcher::pump, "strangevisuals-media");
        thread.setDaemon(true);
        thread.start();
    }

    public static synchronized void stop() {
        started = false;
        track = Track.NONE;
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
    }

    private static void pump() {
        while (started) {
            try {
                ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-Command", "-");
                builder.redirectError(ProcessBuilder.Redirect.DISCARD);
                process = builder.start();

                try (OutputStream out = process.getOutputStream()) {
                    out.write(SCRIPT.getBytes(StandardCharsets.UTF_8));
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        track = parse(line);
                    }
                }
            } catch (IOException e) {
                dev.strangevisuals.StrangeVisuals.LOGGER.debug("MediaWatcher script read failed: {}", e.getMessage());
                track = Track.NONE;
            } finally {
                if (process != null) {
                    process.destroyForcibly();
                    process = null;
                }
            }

            if (started) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static Track parse(String line) {
        if (line == null || line.isBlank()) {
            return Track.NONE;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 6) {
            return Track.NONE;
        }
        String title = parts[0].trim();
        String artist = parts[1].trim();

        if (title.isBlank()) {
            title = artist;
            artist = "";
        }
        return new Track(title, artist, parts[2].trim(), "1".equals(parts[3].trim()),
            parseLong(parts[4]), parseLong(parts[5]));
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
