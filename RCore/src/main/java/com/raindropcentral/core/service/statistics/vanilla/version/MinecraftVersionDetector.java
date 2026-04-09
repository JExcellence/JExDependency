/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.statistics.vanilla.version;

import com.raindropcentral.rplatform.version.ServerEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects and parses the Minecraft server version for vanilla statistics compatibility.
 *
 * <p>This class uses {@link ServerEnvironment} to detect the Minecraft version on startup
 * and parses the version string to extract major and minor version numbers. This information
 * is used to determine which statistics are available in the current version.
 *
 * <p>Supported versions: 1.16 through 1.21+
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MinecraftVersionDetector detector = new MinecraftVersionDetector();
 * int major = detector.getMajorVersion(); // e.g., 20 for 1.20.4
 * int minor = detector.getMinorVersion(); // e.g., 4 for 1.20.4
 * boolean supported = detector.isVersionSupported(); // true for 1.16+
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class MinecraftVersionDetector {

    private static final Logger LOGGER = Logger.getLogger(MinecraftVersionDetector.class.getName());
    
    /**
     * Pattern to extract version numbers from Minecraft version string.
     * Matches formats like "1.20.4", "1.20", "1.21.1", etc.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("1\\.(\\d+)(?:\\.(\\d+))?");
    
    /**
     * Minimum supported major version (1.16).
     */
    private static final int MIN_SUPPORTED_MAJOR = 16;
    
    private final String versionString;
    private final int majorVersion;
    private final int minorVersion;
    
    /**
     * Constructs a new version detector using {@link ServerEnvironment}.
     *
     * <p>The detector immediately parses the version string and extracts
     * major and minor version numbers.
     */
    public MinecraftVersionDetector() {
        final ServerEnvironment env = ServerEnvironment.getInstance();
        this.versionString = env.getMinecraftVersion();
        
        final int[] parsed = parseVersion(versionString);
        this.majorVersion = parsed[0];
        this.minorVersion = parsed[1];
        
        LOGGER.info("Detected Minecraft version: " + versionString + 
                   " (major=" + majorVersion + ", minor=" + minorVersion + ")");
    }
    
    /**
     * Gets the full Minecraft version string.
     *
     * @return the version string (e.g., "git-Paper-123 (MC: 1.20.4)")
     */
    public @NotNull String getVersionString() {
        return versionString;
    }
    
    /**
     * Gets the major version number.
     *
     * <p>For Minecraft 1.20.4, this returns 20.
     *
     * @return the major version number, or 0 if parsing failed
     */
    public int getMajorVersion() {
        return majorVersion;
    }
    
    /**
     * Gets the minor version number.
     *
     * <p>For Minecraft 1.20.4, this returns 4.
     * For Minecraft 1.20, this returns 0.
     *
     * @return the minor version number, or 0 if not present
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Checks if the current version is supported for vanilla statistics collection.
     *
     * <p>Supported versions are 1.16 and above.
     *
     * @return {@code true} if version is 1.16 or higher, {@code false} otherwise
     */
    public boolean isVersionSupported() {
        return majorVersion >= MIN_SUPPORTED_MAJOR;
    }
    
    /**
     * Checks if the current version is at least the specified version.
     *
     * <p>Example: {@code isAtLeast(20, 0)} returns true for 1.20.0 and above.
     *
     * @param major the major version to compare against
     * @param minor the minor version to compare against
     * @return {@code true} if current version is greater than or equal to specified version
     */
    public boolean isAtLeast(final int major, final int minor) {
        if (this.majorVersion > major) {
            return true;
        }
        if (this.majorVersion == major) {
            return this.minorVersion >= minor;
        }
        return false;
    }
    
    /**
     * Checks if the current version is exactly the specified version.
     *
     * @param major the major version to compare against
     * @param minor the minor version to compare against
     * @return {@code true} if current version matches exactly
     */
    public boolean isExactly(final int major, final int minor) {
        return this.majorVersion == major && this.minorVersion == minor;
    }
    
    /**
     * Parses the version string to extract major and minor version numbers.
     *
     * <p>Handles various version string formats:
     * <ul>
     *   <li>"git-Paper-123 (MC: 1.20.4)" → [20, 4]</li>
     *   <li>"1.20.4" → [20, 4]</li>
     *   <li>"1.20" → [20, 0]</li>
     * </ul>
     *
     * @param versionString the version string to parse
     * @return array of [major, minor], or [0, 0] if parsing fails
     */
    private int[] parseVersion(final @NotNull String versionString) {
        final Matcher matcher = VERSION_PATTERN.matcher(versionString);
        
        if (matcher.find()) {
            try {
                final int major = Integer.parseInt(matcher.group(1));
                final int minor = matcher.group(2) != null ? 
                    Integer.parseInt(matcher.group(2)) : 0;
                
                return new int[]{major, minor};
            } catch (final NumberFormatException e) {
                LOGGER.warning("Failed to parse version numbers from: " + versionString);
            }
        } else {
            LOGGER.warning("Could not match version pattern in: " + versionString);
        }
        
        return new int[]{0, 0};
    }
}
