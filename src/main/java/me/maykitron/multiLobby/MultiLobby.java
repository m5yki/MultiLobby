package me.maykitron.multiLobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class MultiLobby extends JavaPlugin {

    private static MultiLobby instance;
    private Lang lang;

    private PlayerHomeManager homeManager;
    private MenuManager menuManager;
    private TeleportUtil teleportUtil;
    private RtpQueueManager rtpQueueManager;
    private WarpManager warpManager;

    private Location spawnLocation;
    private String voidWorldName;
    private double voidYThreshold;
    private BackManager backManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.lang = new Lang(this);
        this.teleportUtil = new TeleportUtil(this);
        this.homeManager = new PlayerHomeManager(this);
        this.warpManager = new WarpManager(this);
        this.menuManager = new MenuManager(this);
        this.rtpQueueManager = new RtpQueueManager(this);
        this.backManager = new BackManager(this);
        getServer().getPluginManager().registerEvents(this.backManager, this);

        loadConfigValues();

        LobbyCommands cmdExecutor = new LobbyCommands(this);
        String[] cmds = {"spawn", "setspawn", "setlobby", "lobby", "warp", "setwarp", "delwarp", "rtp", "kaptan", "home", "multilobby", "discord", "tiktok", "website", "map", "back"};
        for (String c : cmds) {
            if (getCommand(c) != null) getCommand(c).setExecutor(cmdExecutor);
        }

        getServer().getPluginManager().registerEvents(this.homeManager, this);
        getServer().getPluginManager().registerEvents(this.menuManager, this);

        getLogger().info(lang.s("messages.enabled-console", "[MultiLobby] Kaptan ve Warp sistemiyle aktif edildi!"));
    }

    @Override
    public void onDisable() {
        if (teleportUtil != null) teleportUtil.cancelAllTeleports();
        if (lang != null) {
            getLogger().info(lang.s("messages.disabled-console", "[MultiLobby] Devre dışı bırakıldı!"));
        } else {
            getLogger().info("[MultiLobby] Devre dışı bırakıldı!");
        }
    }

    public void loadConfigValues() {
        reloadConfig();
        lang.reload();

        String worldName = getConfig().getString("spawn.world", "world");
        World w = Bukkit.getWorld(worldName);
        double x = getConfig().getDouble("spawn.x", 0);
        double y = getConfig().getDouble("spawn.y", 64);
        double z = getConfig().getDouble("spawn.z", 0);
        float yaw = (float) getConfig().getDouble("spawn.yaw", 0);
        float pitch = (float) getConfig().getDouble("spawn.pitch", 0);
        this.spawnLocation = (w == null) ? null : new Location(w, x, y, z, yaw, pitch);

        this.voidWorldName = getConfig().getString("void-protect.world", "world");
        this.voidYThreshold = getConfig().getDouble("void-protect.y-threshold", 0.0);

        warpManager.loadWarps();
        menuManager.loadMenus();
    }

    public static MultiLobby getInstance() { return instance; }
    public Lang getLang() { return lang; }
    public PlayerHomeManager getHomeManager() { return homeManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public TeleportUtil getTeleportUtil() { return teleportUtil; }
    public RtpQueueManager getRtpQueueManager() { return rtpQueueManager; }
    public WarpManager getWarpManager() { return warpManager; }
    public Location getSpawnLocation() { return spawnLocation; }
    public String getVoidWorldName() { return voidWorldName; }
    public double getVoidYThreshold() { return voidYThreshold; }
    public BackManager getBackManager() { return backManager; }
}