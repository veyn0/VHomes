package dev.veyno.vHomes.util;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utility class for serializing and deserializing Bukkit Locations
 */
public class LocationUtil {

    private static final String SEPARATOR = ";";

    /**
     * Converts a Location to a String representation
     * Format: "world;x;y;z;yaw;pitch"
     *
     * @param location The location to convert
     * @return String representation of the location, null if location is null
     */
    public static String locationToString(Location location) {
        if (location == null) {
            return null;
        }

        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        return location.getWorld().getName() + SEPARATOR +
                location.getX() + SEPARATOR +
                location.getY() + SEPARATOR +
                location.getZ() + SEPARATOR +
                location.getYaw() + SEPARATOR +
                location.getPitch();
    }


    /**
     * Converts a String to a Location
     * Expected format: "world;x;y;z;yaw;pitch" or "world;x;y;z"
     *
     * @param locationString The string representation of the location
     * @return Location object, null if string is null or invalid
     */
    public static Location locationFromString(String locationString) {
        if (locationString == null || locationString.trim().isEmpty()) {
            return null;
        }

        try {
            String[] parts = locationString.split(SEPARATOR);

            if (parts.length < 4) {
                throw new IllegalArgumentException("Invalid location string format. Expected at least 4 parts (world;x;y;z)");
            }

            // Get world
            String worldName = parts[0];
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException("World '" + worldName + "' not found");
            }

            // Parse coordinates
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            // Parse rotation if available
            float yaw = 0.0f;
            float pitch = 0.0f;

            if (parts.length >= 5) {
                yaw = Float.parseFloat(parts[4]);
            }
            if (parts.length >= 6) {
                pitch = Float.parseFloat(parts[5]);
            }

            return new Location(world, x, y, z, yaw, pitch);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in location string: " + locationString, e);
        }
    }

    /**
     * Converts a Location to a block-centered String representation
     * This rounds the coordinates to block centers and removes rotation
     * Format: "world;x;y;z"
     *
     * @param location The location to convert
     * @return String representation of the block location, null if location is null
     */
    public static String blockLocationToString(Location location) {
        if (location == null) {
            return null;
        }

        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location world cannot be null");
        }

        return location.getWorld().getName() + SEPARATOR +
                location.getBlockX() + SEPARATOR +
                location.getBlockY() + SEPARATOR +
                location.getBlockZ();
    }

    /**
     * Converts a String to a block-centered Location
     * The resulting location will be at the center of the block (x.5, y.0, z.5)
     * Expected format: "world;x;y;z"
     *
     * @param locationString The string representation of the block location
     * @return Location object centered on the block, null if string is null or invalid
     */
    public static Location blockLocationFromString(String locationString) {
        Location location = locationFromString(locationString);
        if (location == null) {
            return null;
        }

        // Center the location on the block
        location.setX(location.getBlockX() + 0.5);
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ() + 0.5);
        location.setYaw(0);
        location.setPitch(0);

        return location;
    }


}