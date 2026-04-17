package me.maykitron.multiLobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

public class WarpManager {
    private final MultiLobby plugin;
    private final Map<String, Location> warps = new HashMap<>();

    public WarpManager(MultiLobby plugin) {
        this.plugin = plugin;
    }

    public void loadWarps() {
        warps.clear();
        ConfigurationSection ws = plugin.getConfig().getConfigurationSection("warps");
        if (ws == null) return;
        for (String key : ws.getKeys(false)) {
            ConfigurationSection s = ws.getConfigurationSection(key);
            World w = Bukkit.getWorld(s.getString("world", "world"));
            if (w == null) continue;
            warps.put(key.toLowerCase(), new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), (float) s.getDouble("yaw"), (float) s.getDouble("pitch")));
        }
    }

    public void setWarp(String name, Location loc) {
        String path = "warps." + name.toLowerCase();
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
        plugin.getConfig().set(path + ".icon.material", "COMPASS");
        plugin.getConfig().set(path + ".icon.name", "§e" + name);
        plugin.saveConfig();
        loadWarps();
        plugin.getMenuManager().loadMenus();
    }

    public void delWarp(String name) {
        plugin.getConfig().set("warps." + name.toLowerCase(), null);
        plugin.saveConfig();
        loadWarps();
        plugin.getMenuManager().loadMenus();
    }

    public Location getWarp(String name) { return warps.get(name.toLowerCase()); }
    public Map<String, Location> getAllWarps() { return warps; }
}