package me.maykitron.multiLobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerHomeManager implements Listener {

    private final MultiLobby plugin;
    private final Map<UUID, Location> homeBeds = new HashMap<>();
    private final File playersDir;

    public PlayerHomeManager(MultiLobby plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) playersDir.mkdirs();
    }

    public Location getHome(UUID uuid) {
        return homeBeds.get(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (plugin.getConfig().getBoolean("first-join.spawn", true) && !p.hasPlayedBefore() && plugin.getSpawnLocation() != null) {
            p.teleport(plugin.getSpawnLocation());
            p.sendMessage(plugin.getLang().s("messages.first-join", "Sunucuya hoş geldin!"));
        }

        loadPlayerHome(p.getUniqueId());
        Location bed = homeBeds.get(p.getUniqueId());
        if (bed != null && !isBedBlock(bed.getBlock())) {
            homeBeds.remove(p.getUniqueId());
            savePlayerHome(p.getUniqueId(), null);
            p.sendTitle(plugin.getLang().s("home.bed-broken-title", "§cYatağın kırıldı!"), plugin.getLang().s("home.bed-broken-sub", "§7Yeni bir yatak seç."), 10, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (e.getPlayer().getBedSpawnLocation() == null && plugin.getSpawnLocation() != null) {
            e.setRespawnLocation(plugin.getSpawnLocation());
        }
    }

    @EventHandler
    public void onVoid(PlayerMoveEvent e) {
        if (plugin.getConfig().getBoolean("spawn.cancel-on-move", true) && plugin.getTeleportUtil().hasMoved(e.getPlayer())) {
            plugin.getTeleportUtil().cancelTeleport(e.getPlayer().getUniqueId(), true);
        }

        if (e.getTo() == null || plugin.getSpawnLocation() == null) return;
        if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(plugin.getVoidWorldName())) return;

        if (e.getTo().getY() <= plugin.getVoidYThreshold()) {
            Player p = e.getPlayer();
            p.setVelocity(new Vector(0, 0, 0));
            p.setFallDistance(0f);
            p.teleport(plugin.getSpawnLocation());
        }
    }

    @EventHandler
    public void onBedRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null || !isBedBlock(b)) return;

        Player p = e.getPlayer();
        Location foot = normalizeBedFoot(b.getLocation());
        if (foot == null) return;

        Location finalLoc = foot.clone().add(0.5, 0, 0.5);
        UUID newOwner = p.getUniqueId();

        // Yatak Ele Geçirme Kontrolü
        UUID oldOwner = null;
        for (Map.Entry<UUID, Location> entry : homeBeds.entrySet()) {
            if (sameBlock(entry.getValue(), finalLoc) && !entry.getKey().equals(newOwner)) {
                oldOwner = entry.getKey();
                break;
            }
        }

        if (oldOwner != null) {
            Player oldPlayer = Bukkit.getPlayer(oldOwner);
            if (oldPlayer != null && oldPlayer.isOnline()) {
                String msg = plugin.getLang().s("home.bed-taken", "§6[!] §cYatağın %player% tarafından ele geçirildi!").replace("%player%", p.getName());
                oldPlayer.sendMessage(msg);
                oldPlayer.playSound(oldPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
            }
            homeBeds.remove(oldOwner);
            savePlayerHome(oldOwner, null);
        }

        homeBeds.put(newOwner, finalLoc);
        savePlayerHome(newOwner, finalLoc);

        p.getWorld().playSound(finalLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
        p.sendTitle(plugin.getLang().s("home.bed-set-title", "§aYatak Ayarlandı!"), plugin.getLang().s("home.bed-set-sub", "§7Artık burası evin."), 10, 40, 10);
    }

    @EventHandler
    public void onBedBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!isBedBlock(b)) return;
        Location foot = normalizeBedFoot(b.getLocation());
        if (foot == null) return;

        Location centerFoot = foot.clone().add(0.5, 0, 0.5);
        UUID owner = null;
        for (Map.Entry<UUID, Location> entry : homeBeds.entrySet()) {
            if (sameBlock(entry.getValue(), centerFoot)) {
                owner = entry.getKey();
                break;
            }
        }

        if (owner != null) {
            homeBeds.remove(owner);
            savePlayerHome(owner, null);
            Player p = Bukkit.getPlayer(owner);
            if (p != null && p.isOnline()) {
                p.sendTitle(plugin.getLang().s("home.bed-broken-title", "§cYatağın kırıldı!"), plugin.getLang().s("home.bed-broken-sub", "§7Yeni bir yatak seç."), 10, 50, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
            }
        }
    }

    private void loadPlayerHome(UUID uuid) {
        try {
            File f = new File(playersDir, uuid.toString() + ".yml");
            if (!f.exists()) return;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (!y.isConfigurationSection("home")) return;
            org.bukkit.World world = Bukkit.getWorld(y.getString("home.world", ""));
            if (world == null) return;
            homeBeds.put(uuid, new Location(world, y.getDouble("home.x"), y.getDouble("home.y"), y.getDouble("home.z"), (float) y.getDouble("home.yaw", 0), (float) y.getDouble("home.pitch", 0)));
        } catch (Exception ignored) {}
    }

    private void savePlayerHome(UUID uuid, Location loc) {
        try {
            File f = new File(playersDir, uuid.toString() + ".yml");
            YamlConfiguration y = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
            if (loc == null) {
                y.set("home", null);
            } else {
                y.set("home.world", loc.getWorld().getName());
                y.set("home.x", loc.getX());
                y.set("home.y", loc.getY());
                y.set("home.z", loc.getZ());
                y.set("home.yaw", loc.getYaw());
                y.set("home.pitch", loc.getPitch());
            }
            y.save(f);
        } catch (Exception ignored) {}
    }

    private boolean isBedBlock(Block b) { return b != null && b.getType().name().endsWith("_BED"); }

    private Location normalizeBedFoot(Location loc) {
        if (loc == null || loc.getBlock() == null || !isBedBlock(loc.getBlock())) return null;
        Block part = loc.getBlock();
        if (part.getBlockData() instanceof Bed bedData) {
            Block footBlock = (bedData.getPart() == Bed.Part.FOOT) ? part : part.getRelative(bedData.getFacing().getOppositeFace());
            return footBlock.getLocation();
        }
        return loc;
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }
}