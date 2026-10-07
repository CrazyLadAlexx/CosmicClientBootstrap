package me.alex.cosmicclientbootstrap;

import me.alex.cosmicclientbootstrap.command.PacketCommand;
import me.alex.cosmicclientbootstrap.packet.PacketJoinListener;
import me.alex.cosmicclientbootstrap.packet.PacketService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CosmicClientBootstrap extends JavaPlugin {

    private PacketService packets;

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, PacketService.CHANNEL);
        packets = new PacketService(this);

        PacketJoinListener listener = new PacketJoinListener(this, packets);
        getServer().getPluginManager().registerEvents(listener, this);

        PluginCommand command = getCommand("packets");
        if (command == null) {
            throw new IllegalStateException("The packets command is missing from plugin.yml");
        }
        PacketCommand executor = new PacketCommand(this, packets, listener);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getOnlinePlayers().forEach(listener::bootstrap);
        getLogger().info("CosmicClientBootstrap by CrazyLadAlex enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, PacketService.CHANNEL);
        packets = null;
    }

    public PacketService getPacketService() {
        if (packets == null) {
            throw new IllegalStateException("CosmicClientBootstrap is not enabled");
        }
        return packets;
    }
}
