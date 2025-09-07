package me.yourname.sleepyhomes.data;

import me.yourname.sleepyhomes.SleepyHomes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
public class HomeManager {
    private final SleepyHomes plugin;
    private final File file;
    private final YamlConfiguration cfg;
    public HomeManager(SleepyHomes plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        if (!file.exists()) {
            plugin.saveResource("homes.yml", false);
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }
    public Location getHome(UUID player, int slotNumber) {
        String path = "players." + player.toString() + "." + slotNumber;
        if (!cfg.contains(path)) return null;
        String world = cfg.getString(path + ".world");
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        if (world == null) return null;
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
    public void setHome(UUID player, int slotNumber, Location loc) {
        String path = "players." + player.toString() + "." + slotNumber;
        cfg.set(path + ".world", loc.getWorld().getName());
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", loc.getYaw());
        cfg.set(path + ".pitch", loc.getPitch());
        save();
    }
    public void deleteHome(UUID player, int slotNumber) {
        String path = "players." + player.toString() + "." + slotNumber;
        cfg.set(path, null);
        save();
    }
    public boolean hasHome(UUID player, int slotNumber) {
        return cfg.contains("players." + player.toString() + "." + slotNumber);
    }
    public void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }
    public void saveAll() {
        save();
    }
}
