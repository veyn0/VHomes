package dev.veyno.vHomes.home;

import org.bukkit.Location;

import java.time.LocalDateTime;
import java.util.UUID;

public record HomeDTO(
        UUID homeId,
        UUID ownerId,
        Location location,
        String displayName,
        LocalDateTime updatedAt
) {
}
