package me.alex.cosmicclientbootstrap.command;

import me.alex.cosmicclientbootstrap.packet.PacketJoinListener;
import me.alex.cosmicclientbootstrap.packet.PacketService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class PacketCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "bootstrap", "feature", "permissions", "worldname", "highlight", "mapimage", "help");

    private final Plugin plugin;
    private final PacketService packets;
    private final PacketJoinListener listener;

    public PacketCommand(Plugin plugin, PacketService packets, PacketJoinListener listener) {
        this.plugin = plugin;
        this.packets = packets;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cosmic.admin")) {
            message(sender, "&cYou do not have permission to use this command.");
            return true;
        }
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            usage(sender, label);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        int targetIndex = targetIndex(action);
        if (targetIndex < 0 || args.length < targetIndex || args.length > targetIndex + 1) {
            usage(sender, label);
            return true;
        }
        Player target = target(sender, args, targetIndex);
        if (target == null) {
            return true;
        }

        try {
            switch (action) {
                case "bootstrap":
                    listener.bootstrap(target);
                    break;
                case "feature":
                    packets.legacyFeatureSet(target);
                    packets.featureSet(target);
                    break;
                case "permissions":
                    packets.permissions(target);
                    break;
                case "worldname":
                    if (args[1].trim().isEmpty()) {
                        throw new IllegalArgumentException("World name must not be empty.");
                    }
                    packets.worldName(target, args[1]);
                    break;
                case "highlight":
                    if (!highlight(sender, target, args)) {
                        return true;
                    }
                    break;
                case "mapimage":
                    mapImage(target, args);
                    break;
                default:
                    throw new IllegalStateException("Unhandled packet command: " + action);
            }
            message(sender, "&aRequested " + action + " for &e" + target.getName() + "&a.");
        } catch (NumberFormatException exception) {
            message(sender, "&cInvalid number. Use /" + label + " help for syntax.");
        } catch (IllegalArgumentException exception) {
            message(sender, "&c" + exception.getMessage());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Packet command " + action + " failed for " + target.getName(), exception);
            message(sender, "&cThe packet request failed. Check the server console for details.");
        }
        return true;
    }

    private boolean highlight(CommandSender sender, Player target, String[] args) {
        int operation = Integer.parseInt(args[1]);
        double min = Double.parseDouble(args[2]);
        double max = Double.parseDouble(args[3]);
        if (!Double.isFinite(min) || !Double.isFinite(max) || min < 0.0D || max < min) {
            throw new IllegalArgumentException("Intensity must be finite, non-negative, and min must be at most max.");
        }
        Block block = target.getTargetBlock(Collections.singleton(Material.AIR), 10);
        if (block == null || block.getType() == Material.AIR) {
            message(sender, "&c" + target.getName() + " must look at a block within 10 blocks.");
            return false;
        }
        packets.permissions(target);
        if (packets.highlight(target, block.getLocation(), operation, 0xFF0000, min, max)
                == PacketService.HighlightDelivery.NOT_SENT) {
            message(sender, "&cCould not send the highlight packet to " + target.getName() + ".");
            return false;
        }
        return true;
    }

    private void mapImage(Player target, String[] args) {
        URI url = URI.create(args[1]);
        if (url.getHost() == null || !("https".equalsIgnoreCase(url.getScheme())
                || "http".equalsIgnoreCase(url.getScheme()))) {
            throw new IllegalArgumentException("Map image URL must use http or https and include a host.");
        }
        int width = Integer.parseInt(args[2]);
        int height = Integer.parseInt(args[3]);
        int x = Integer.parseInt(args[4]);
        int z = Integer.parseInt(args[5]);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map image width and height must be positive.");
        }
        packets.permissions(target);
        packets.mapImage(target, url.toString(), width, height, x, z);
    }

    private Player target(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player player = plugin.getServer().getPlayerExact(args[index]);
            if (player == null) {
                message(sender, "&cPlayer not found: " + args[index]);
            }
            return player;
        }
        if (sender instanceof Player) {
            return (Player) sender;
        }
        message(sender, "&cConsole must provide a player.");
        return null;
    }

    private int targetIndex(String action) {
        switch (action) {
            case "bootstrap":
            case "feature":
            case "permissions":
                return 1;
            case "worldname":
                return 2;
            case "highlight":
                return 4;
            case "mapimage":
                return 6;
            default:
                return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cosmic.admin") || args.length == 0) {
            return Collections.emptyList();
        }
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            candidates.addAll(SUBCOMMANDS);
        } else if (args.length == targetIndex(args[0].toLowerCase(Locale.ROOT)) + 1) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!(sender instanceof Player) || ((Player) sender).canSee(player)) {
                    candidates.add(player.getName());
                }
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        candidates.removeIf(value -> !value.toLowerCase(Locale.ROOT).startsWith(prefix));
        return candidates;
    }

    private void usage(CommandSender sender, String label) {
        message(sender, "&e/" + label + " bootstrap [player]");
        message(sender, "&e/" + label + " feature [player]");
        message(sender, "&e/" + label + " permissions [player]");
        message(sender, "&e/" + label + " worldname <name> [player]");
        message(sender, "&e/" + label + " highlight <operation> <min> <max> [player]");
        message(sender, "&e/" + label + " mapimage <url> <width> <height> <topLeftX> <topLeftZ> [player]");
    }

    private void message(CommandSender sender, String text) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }
}
