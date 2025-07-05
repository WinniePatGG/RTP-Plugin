package de.winniepat.rtp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class RTPCommand implements CommandExecutor {
    private final Rtp plugin;
    private final Random random = new Random();

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownTime = 300_000;

    public RTPCommand(Rtp plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (!player.hasPermission("rtp.bypasscooldown")) {
            if (cooldowns.containsKey(uuid)) {
                long lastUsed = cooldowns.get(uuid);
                long remaining = cooldownTime - (currentTime - lastUsed);
                if (remaining > 0) {
                    long seconds = remaining / 1000;
                    player.sendMessage("§cYou must wait " + seconds + " seconds before using /rtp again.");
                    return true;
                }
            }
        }

        World world = player.getWorld();

        double centerX = -640;
        double centerZ = -830;
        double radius = 5000;

        Location tpLocation = null;

        for (int i = 0; i < 10; i++) {
            double x = centerX + (random.nextDouble() * radius * 2) - radius;
            double z = centerZ + (random.nextDouble() * radius * 2) - radius;

            int y = world.getHighestBlockYAt((int) x, (int) z);
            Location loc = new Location(world, x + 0.5, y, z + 0.5);

            Material blockType = world.getBlockAt(loc).getType();
            if (!blockType.equals(Material.AIR) && !blockType.equals(Material.CAVE_AIR) && !blockType.equals(Material.VOID_AIR)) {
                tpLocation = loc;
                break;
            }
        }

        if (tpLocation != null) {
            player.teleport(tpLocation);
            player.sendMessage("§aYou have been teleported randomly!");

            if (!player.hasPermission("rtp.bypasscooldown")) {
                cooldowns.put(uuid, currentTime);
            }
        } else {
            player.sendMessage("§cFailed to find a safe location. Try again.");
        }

        return true;
    }
}