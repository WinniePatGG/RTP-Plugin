package de.winniepat.rtp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RTPCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RTPCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!p.hasPermission("rtp.use")) {
            p.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(p.getUniqueId())) {
            long expireTime = cooldowns.get(p.getUniqueId());
            if (currentTime < expireTime) {
                long remaining = (expireTime - currentTime) / 1000;
                p.sendMessage(ChatColor.RED + "You must wait " + remaining + " seconds before using RTP again.");
                return true;
            }
        }

        if (!p.hasPermission("rtp.bypass.cooldown")) {
            cooldowns.put(p.getUniqueId(), currentTime + (5 * 60 * 1000));
        }

        World world = p.getWorld();
        String world_name = world.getName();

        p.sendMessage(ChatColor.YELLOW + "Searching for a safe location in " + world_name + "...");

        new BukkitRunnable() {
            @Override
            public void run() {
                Location safeLoc = null;
                int radius = 2000;
                int attempts = 20;

                switch (world.getEnvironment()) {
                    case NORMAL -> safeLoc = findSafeLocationOverworld(world, radius, attempts);
                    case NETHER -> safeLoc = findSafeLocationNether(world, radius, attempts);
                    case THE_END -> sender.sendMessage("Not yet implemented");
                    default -> {}
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
        for (int i = 0; i < attempts; i++) {
            int x = -radius + (int) (Math.random() * radius * 2);
            int z = -radius + (int) (Math.random() * radius * 2);
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            Block block = world.getBlockAt(x, y - 1, z);

            if (!block.getType().isSolid()) continue;

            if (loc.getBlock().getType() == Material.AIR && loc.clone().add(0,1,0).getBlock().getType() == Material.AIR) {
                return loc;
            }
        }
        return null;
    }

    private Location findSafeLocationNether(World world, int radius, int attempts) {
        for (int i = 0; i < attempts; i++) {
            int x = -radius + (int) (Math.random() * radius * 2);
            int z = -radius + (int) (Math.random() * radius * 2);

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
}
