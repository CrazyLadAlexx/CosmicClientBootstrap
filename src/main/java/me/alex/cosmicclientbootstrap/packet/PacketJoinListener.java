package me.alex.cosmicclientbootstrap.packet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class PacketJoinListener implements Listener {

    private final Plugin plugin;
    private final PacketService packets;

    public PacketJoinListener(Plugin plugin, PacketService packets) {
        this.plugin = plugin;
        this.packets = packets;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        bootstrap(event.getPlayer());
    }

    public void bootstrap(Player player) {
        if (!player.isOnline()) {
            return;
        }
        packets.legacyFeatureSet(player);
        schedule(player, 10L, () -> {
            packets.featureSet(player);
            packets.headers(player);
        });
        schedule(player, 20L, () -> {
            packets.permissions(player);
            packets.register(player);
            // Retain the repeated feature packet used by the original handshake.
            packets.featureSet(player);
            packets.featureSet(player);
            packets.cooldowns(player);
        });
        schedule(player, 40L, () -> {
            packets.featureSet(player);
            packets.headers(player);
        });
        schedule(player, 100L, () -> packets.worldName(player, player.getWorld().getName()));
    }

    private void schedule(Player player, long delay, Runnable task) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                task.run();
            }
        }, delay);
    }
}
