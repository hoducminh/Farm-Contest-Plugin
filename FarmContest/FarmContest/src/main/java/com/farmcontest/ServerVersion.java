package com.farmcontest;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime Minecraft version detection.
 *
 * <p>Multi-version design: a single JAR runs on every server from 1.20 up to
 * 1.21.11 (and beyond). Use {@link #isAtLeast} to branch logic by version
 * without needing multiple build artifacts.
 *
 * <pre>
 *   // Usage example
 *   if (ServerVersion.isAtLeast(1, 21, 8)) {
 *       // Use an API only available from 1.21.8+
 *   }
 * </pre>
 */
public final class ServerVersion {

    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;

    static {
        // Bukkit version string: "1.21.8-R0.1-SNAPSHOT"
        String raw = Bukkit.getBukkitVersion();
        Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.?(\\d*)").matcher(raw);
        if (m.find()) {
            MAJOR = Integer.parseInt(m.group(1));
            MINOR = Integer.parseInt(m.group(2));
            String patch = m.group(3);
            PATCH = (patch != null && !patch.isEmpty()) ? Integer.parseInt(patch) : 0;
        } else {
            // Fallback if parsing fails — assume 1.21.0
            MAJOR = 1;
            MINOR = 21;
            PATCH = 0;
        }
    }

    private ServerVersion() {}

    // ── Public API ────────────────────────────────────────────

    /** @return true if server >= major.minor */
    public static boolean isAtLeast(int major, int minor) {
        if (MAJOR != major) return MAJOR > major;
        return MINOR >= minor;
    }

    /** @return true if server >= major.minor.patch */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (MAJOR != major) return MAJOR > major;
        if (MINOR != minor) return MINOR > minor;
        return PATCH >= patch;
    }

    public static int getMajor()  { return MAJOR; }
    public static int getMinor()  { return MINOR; }
    public static int getPatch()  { return PATCH; }

    /** Returns the string "1.21.8", or "1.21.0" if patch = 0. */
    public static String asString() {
        return MAJOR + "." + MINOR + (PATCH > 0 ? "." + PATCH : "");
    }

    /**
     * Logs compat info to the console when the plugin starts.
     * Warns if running below the supported version.
     */
    public static void logCompat(java.util.logging.Logger logger,
                                  int minMajor, int minMinor) {
        String detected = "Minecraft " + asString() + " (Paper)";
        if (!isAtLeast(minMajor, minMinor)) {
            logger.warning("[VersionCompat] " + detected
                + " < " + minMajor + "." + minMinor
                + " — the plugin may not work correctly!");
        } else {
            logger.info("[VersionCompat] Detected " + detected + " — OK");
        }
    }
}
