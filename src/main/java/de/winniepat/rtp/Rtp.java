package de.winniepat.rtp;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Rtp extends JavaPlugin {

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("rtp")).setExecutor(new RTPCommand(this));
        getLogger().info("Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled");
    }
}
