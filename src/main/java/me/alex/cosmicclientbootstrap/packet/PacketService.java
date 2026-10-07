package me.alex.cosmicclientbootstrap.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.happy.cosmicclientapi.CosmicClientAPI;
import me.happy.cosmicclientapi.UserManager;
import me.happy.cosmicclientapi.packet.client.ClientPacket;
import me.happy.cosmicclientapi.packet.client.SFeatureSetPacket;
import me.happy.cosmicclientapi.packet.client.SHighlightBlockPacket;
import me.happy.cosmicclientapi.packet.client.SItemCooldownsListPacket;
import me.happy.cosmicclientapi.packet.client.SPlayerHeadersPacket;
import me.happy.cosmicclientapi.packet.client.SSetXPPacket;
import me.happy.cosmicclientapi.packet.client.SShowXPBarPacket;
import me.happy.cosmicclientapi.packet.client.SXPTextPacket;
import me.happy.cosmicclientapi.utils.PacketUtils;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.demonicpvp.demonicclientcomms.packet.CosmicPacketHandler;
import org.demonicpvp.demonicclientcomms.packet.clientbound.IClientBoundPacket;
import org.demonicpvp.demonicclientcomms.packet.clientbound.SFeatureSet;
import org.demonicpvp.demonicclientcomms.packet.clientbound.SSetMapImagePacket;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PacketService {

    public enum HighlightDelivery {
        NOT_SENT,
        COSMIC_API
    }

    public static final String CHANNEL = "cosmic";
    public static final String FEATURE_SET = "COSMICPRISONS";

    private static final Field cooldowns = field(SItemCooldownsListPacket.class, "cooldowns");
    private static final Field playerHeaders = field(SPlayerHeadersPacket.class, "playerHeaders");
    private static final Field headersPatch = field(SPlayerHeadersPacket.class, "patch");
    private static final Field xpBarValue = field(SShowXPBarPacket.class, "value");
    private static final Field xpBarText = field(SXPTextPacket.class, "text");
    private static final Field xpLevel = field(SSetXPPacket.class, "level");
    private static final Field xpCurrent = field(SSetXPPacket.class, "xp");
    private static final Field xpTotal = field(SSetXPPacket.class, "total");
    private static final Field xpColor = field(SSetXPPacket.class, "color");
    private static final Field xpAnimation = field(SSetXPPacket.class, "animation");

    private final Plugin plugin;

    public PacketService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void permissions(Player player) {
        if (valid(player))
            CosmicPacketHandler.sendPermissions(player);
    }

    public void featureSet(Player player) {
        cosmic(player, new SFeatureSetPacket(FEATURE_SET), SFeatureSetPacket.class);
    }

    public void legacyFeatureSet(Player player) {
        demonic(player, new SFeatureSet(FEATURE_SET));
    }

    public void worldName(Player player, String name) {
        if (name == null || name.trim().isEmpty())
            return;

        me.happy.cosmicclientapi.packet.client.SSetWorldNamePacket packet =
                new me.happy.cosmicclientapi.packet.client.SSetWorldNamePacket(
                        name.trim(), Collections.emptyList(), (byte) 0, (byte) 0);
        cosmic(player, plugin, packet,
                me.happy.cosmicclientapi.packet.client.SSetWorldNamePacket.class);
    }

    public void register(Player player) {
        if (!valid(player))
            return;

        UserManager users = CosmicClientAPI.getUserManager();
        if (users != null && !users.isRegistered(player))
            users.register(player);
    }

    public void cooldowns(Player player) {
        SItemCooldownsListPacket packet = new SItemCooldownsListPacket();
        set(cooldowns, packet, Collections.emptyMap());
        cosmic(player, packet, SItemCooldownsListPacket.class);
    }

    public void headers(Player player) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (Player online : Bukkit.getOnlinePlayers())
            headers.put(online.getName(), Collections.emptyList());

        SPlayerHeadersPacket packet = new SPlayerHeadersPacket();
        set(playerHeaders, packet, headers);
        set(headersPatch, packet, false);
        cosmic(player, packet, SPlayerHeadersPacket.class);
    }

    public void xpBar(Player player, boolean visible) {
        SShowXPBarPacket packet = new SShowXPBarPacket();
        set(xpBarValue, packet, visible);
        cosmic(player, packet, SShowXPBarPacket.class);
    }

    public void xpText(Player player, String text) {
        if (text == null || text.trim().isEmpty())
            return;

        SXPTextPacket packet = new SXPTextPacket();
        set(xpBarText, packet, text);
        cosmic(player, packet, SXPTextPacket.class);
    }

    public void setXp(Player player, int level, long xp, long total, String color, boolean animation) {
        SSetXPPacket packet = new SSetXPPacket();
        set(xpLevel, packet, Math.max(1, level));
        set(xpCurrent, packet, Math.max(0L, xp));
        set(xpTotal, packet, Math.max(1L, total));
        set(xpColor, packet, color);
        set(xpAnimation, packet, animation);
        cosmic(player, packet, SSetXPPacket.class);
    }

    public void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!valid(player))
            return;

        PacketContainer times = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TITLE);
        times.getTitleActions().write(0, EnumWrappers.TitleAction.TIMES);
        times.getIntegers().write(0, fadeIn).write(1, stay).write(2, fadeOut);
        protocol(player, times);

        protocol(player, titleText(EnumWrappers.TitleAction.TITLE, title == null ? "" : title));

        if (subtitle != null && !subtitle.trim().isEmpty())
            protocol(player, titleText(EnumWrappers.TitleAction.SUBTITLE, subtitle));
    }

    public void mapImage(Player player, String url, int width, int height, int x, int z) {
        if (url != null && !url.trim().isEmpty())
            demonic(player, new SSetMapImagePacket(url.trim(), width, height, x, z));
    }

    public HighlightDelivery highlight(Player player, Location location, int operation, int colorCode, double min, double max) {
        if (!valid(player) || location == null || location.getWorld() == null
                || !player.getWorld().equals(location.getWorld()))
            return HighlightDelivery.NOT_SENT;

        CosmicClientAPI sender = CosmicClientAPI.getInstance();
        if (sender == null)
            return HighlightDelivery.NOT_SENT;

        // CosmicClientAPI expects an RGB colorCode, without the legacy alpha byte.
        cosmic(player, plugin, new SHighlightBlockPacket(operation, "red", colorCode & 0xFFFFFF,
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), min, max),
                SHighlightBlockPacket.class);
        return HighlightDelivery.COSMIC_API;
    }

    public void demonic(Player player, IClientBoundPacket packet) {
        if (valid(player) && packet != null)
            CosmicPacketHandler.sendPacketToPlayer(player, packet);
    }

    public <T extends ClientPacket> void cosmic(Player player, T packet, Class<T> packetClass) {
        cosmic(player, plugin, packet, packetClass);
    }

    public int rgba(int red, int green, int blue, int alpha) {
        return CosmicPacketHandler.rgbaToInt(color(red), color(green), color(blue), color(alpha));
    }

    private int color(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private boolean valid(Player player) {
        return player != null && player.isOnline();
    }

    private PacketContainer titleText(EnumWrappers.TitleAction action, String text) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TITLE);
        packet.getTitleActions().write(0, action);
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(text));
        return packet;
    }

    private void protocol(Player player, PacketContainer packet) {
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, false);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Unable to send " + packet.getType() + " to " + player.getName(), exception);
        }
    }

    private <T extends ClientPacket> void cosmic(Player player, Plugin sender, T packet, Class<T> packetClass) {
        if (!valid(player) || sender == null || packet == null || packetClass == null)
            return;

        byte[] payload = PacketUtils.getFormattedPacket(PacketUtils.encryptPacket(packet, packetClass));
        player.sendPluginMessage(sender, CHANNEL, payload);
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access " + type.getSimpleName() + "." + name, exception);
        }
    }

    private static void set(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to write " + field.getName(), exception);
        }
    }
}
