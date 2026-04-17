package me.maykitron.multiLobby;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Lang {
    private final JavaPlugin plugin;
    private YamlConfiguration lang;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        try {
            File f = new File(plugin.getDataFolder(), "lang.yml");
            if (!f.exists()) plugin.saveResource("lang.yml", false);
            lang = YamlConfiguration.loadConfiguration(f);
            try (InputStreamReader r = new InputStreamReader(
                    plugin.getResource("lang.yml"), StandardCharsets.UTF_8)) {
                if (r != null) {
                    YamlConfiguration def = YamlConfiguration.loadConfiguration(r);
                    lang.setDefaults(def);
                    lang.options().copyDefaults(true);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("lang.yml yüklenemedi: " + e.getMessage());
            lang = new YamlConfiguration();
        }
    }

    public String s(String path) { String v = lang.getString(path); return v == null ? "" : v; }
    public String s(String path, String def) { String v = lang.getString(path); return v == null ? def : v; }
    public List<String> ls(String path) { return lang.getStringList(path); }
    public int i(String path, int def) { return lang.getInt(path, def); }
    public boolean b(String path, boolean def) { return lang.getBoolean(path, def); }
}