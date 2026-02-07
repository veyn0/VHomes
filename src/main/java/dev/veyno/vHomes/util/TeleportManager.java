package dev.veyno.vHomes.util;


import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final Plugin plugin;
    private final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();

    public TeleportManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public TeleportResult teleportToPlayer(Player player, Player target, int warmupSeconds) {
        return teleportToLocation(player, target.getLocation(), warmupSeconds);
    }


    public TeleportResult teleportToLocation(Player player, Location destination, int warmupSeconds) {
        UUID uuid = player.getUniqueId();

        if (activeTeleports.containsKey(uuid)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("teleport.already-teleporting", "<red>you are already teleporting")));
            return new TeleportResult(false);
        }

        Location startBlock = player.getLocation().getBlock().getLocation();

        TeleportTask task = new TeleportTask(player, destination, startBlock, warmupSeconds);
        activeTeleports.put(uuid, task);
        task.start();

        player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("teleport.warmup-start")
                .replace("{SECONDS}", String.valueOf(warmupSeconds))));

        return new TeleportResult(true);
    }


    public boolean cancelTeleport(Player player) {
        UUID uuid = player.getUniqueId();
        TeleportTask task = activeTeleports.remove(uuid);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }


    public boolean isTeleporting(Player player) {
        return activeTeleports.containsKey(player.getUniqueId());
    }


    private class TeleportTask {
        private final Player player;
        private final Location destination;
        private final Location startBlock;
        private final int warmupSeconds;
        private final long startTime;
        private ScheduledTask task;
        private int lastSecondAnnounced;

        public TeleportTask(Player player, Location destination, Location startBlock, int warmupSeconds) {
            this.player = player;
            this.destination = destination;
            this.startBlock = startBlock;
            this.warmupSeconds = warmupSeconds;
            this.startTime = System.currentTimeMillis();
            this.lastSecondAnnounced = warmupSeconds;
        }

        public void start() {
            task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location currentBlock = player.getLocation().getBlock().getLocation();
                if (!currentBlock.equals(startBlock)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("teleport.cancelled-moved")));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.8f);
                    cancel();
                    return;
                }

                long elapsedMillis = System.currentTimeMillis() - startTime;
                long elapsedSeconds = elapsedMillis / 1000;
                int remainingSeconds = warmupSeconds - (int) elapsedSeconds;

                if (elapsedMillis >= warmupSeconds * 1000L) {
                    player.teleportAsync(destination);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("teleport.success")));
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);

                    activeTeleports.remove(player.getUniqueId());

                    scheduledTask.cancel();
                } else if (remainingSeconds < lastSecondAnnounced && remainingSeconds >= 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("teleport.countdown")
                            .replace("{SECONDS}", String.valueOf(remainingSeconds))));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    lastSecondAnnounced = remainingSeconds;
                }
            }, null, 1L, 1L);
        }

        public void cancel() {
            if (task != null) {
                task.cancel();
            }
            activeTeleports.remove(player.getUniqueId());
        }
    }


    public static class TeleportResult {
        private final boolean success;

        public TeleportResult(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}