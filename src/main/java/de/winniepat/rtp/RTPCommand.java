package de.winniepat.rtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RTPCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RTPCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!p.hasPermission("rtp.use")) {
            p.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        if (!p.hasPermission("rtp.bypass.cooldown")) {
            long expireTime = cooldowns.getOrDefault(p.getUniqueId(), 0L);
            if (currentTime < expireTime) {
                long remaining = (expireTime - currentTime) / 1000;
                p.sendMessage(ChatColor.RED + "You must wait " + remaining + " seconds before using RTP again.");
                return true;
            }
            cooldowns.put(p.getUniqueId(), currentTime + (5 * 60 * 1000));
        }

        World world = p.getWorld();
        p.sendMessage(ChatColor.YELLOW + "Searching for a safe location in " + world.getName() + "...");

        new BukkitRunnable() {
            @Override
            public void run() {
                Location safeLoc = null;
                int radius = 4000;
                int attempts = 20;

                switch (world.getEnvironment()) {
                    case NORMAL -> safeLoc = findSafeLocationOverworld(world, radius, attempts);
                    case NETHER -> safeLoc = findSafeLocationNether(world, radius, attempts);
                    case THE_END -> safeLoc = findSafeLocationEnd(world, radius, attempts);
                }

                if (safeLoc != null) {
                    Location finalLoc = safeLoc;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.teleport(finalLoc);
                        p.sendMessage(ChatColor.GREEN + "Teleported to a safe location!");
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.sendMessage(ChatColor.RED + "Could not find a safe location. Try again later.");
                    });
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }

    private Location findSafeLocationOverworld(World world, int radius, int attempts) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();

        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

            int worldX = center.getBlockX() + x;
            int worldZ = center.getBlockZ() + z;
            int y = world.getHighestBlockYAt(worldX, worldZ) + 1;

            Location loc = new Location(world, worldX + 0.5, y, worldZ + 0.5);

            if (!border.isInside(loc)) continue;

            Block blockBelow = world.getBlockAt(worldX, y - 1, worldZ);
            if (!blockBelow.getType().isSolid()) continue;

            Material below = blockBelow.getType();
            if (below == Material.LAVA || below == Material.WATER || below == Material.CACTUS) continue;

            Block block = loc.getBlock();
            Block blockAbove = block.getRelative(BlockFace.UP);

            if (block.getType() == Material.AIR && blockAbove.getType() == Material.AIR) {
                return loc;
            }
        }
        return null;
    }

    private Location findSafeLocationNether(World world, int radius, int attempts) {
        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

            for (int y = 120; y > 10; y--) {
                Location loc = new Location(world, x + 0.5, y, z + 0.5);
                Block block = loc.getBlock();

                if (block.getType().isSolid()
                        && block.getType() != Material.LAVA
                        && block.getType() != Material.MAGMA_BLOCK) {

                    Block above = block.getRelative(BlockFace.UP);
                    Block above2 = above.getRelative(BlockFace.UP);

                    if (above.getType() == Material.AIR && above2.getType() == Material.AIR) {
                        return above.getLocation().add(0.5, 0, 0.5);
                    }
                }
            }
        }
        return null;
    }

    private Location findSafeLocationEnd(World world, int radius, int attempts) {
        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            Block blockBelow = world.getBlockAt(x, y - 1, z);

            if (blockBelow.getType() == Material.END_STONE) {
                Block block = loc.getBlock();
                Block blockAbove = block.getRelative(BlockFace.UP);

                if (block.getType() == Material.AIR && blockAbove.getType() == Material.AIR) {
                    return loc;
                }
            }
        }
        return null;
    }
}
