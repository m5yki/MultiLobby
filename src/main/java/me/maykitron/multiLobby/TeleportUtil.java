package me.maykitron.multiLobby;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportUtil {

    private final MultiLobby plugin;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingCountdowns = new HashMap<>();
    private final Map<UUID, Location> pendingStartLoc = new HashMap<>();

    public TeleportUtil(MultiLobby plugin) {
        this.plugin = plugin;
    }

    public void startWarmupTeleport(Player p, Location target, String reason, Particle override, String style) {
        final UUID id = p.getUniqueId();
        final int total = Math.max(1, plugin.getConfig().getInt("spawn.warmup-seconds", 3));
        final long start = System.currentTimeMillis();

        cancelTeleport(id, false);
        pendingStartLoc.put(id, p.getLocation().clone());

        BukkitTask countdown = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player pl = Bukkit.getPlayer(id);
            if (pl == null || !pl.isOnline()) return;

            long elapsed = (System.currentTimeMillis() - start) / 1000L;
            long remain = Math.max(0, total - elapsed);

            if (remain > 0) pl.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getLang().s("info.teleport-wait").replace("%remain%", String.valueOf(remain))));
            else pl.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getLang().s("info.teleport-now")));

            float pitch = 1.0f + (float) Math.min(total, elapsed) * 0.08f;
            pl.playSound(pl.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, pitch);
            pl.getWorld().spawnParticle(Particle.PORTAL, pl.getLocation().add(0, 1, 0), 8, .25, .25, .25, .01);
        }, 0L, 20L);
        pendingCountdowns.put(id, countdown);

        BukkitTask tpTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(id);
            BukkitTask c = pendingCountdowns.remove(id); if (c != null) c.cancel();
            pendingStartLoc.remove(id);
            Player pl = Bukkit.getPlayer(id); if (pl == null || !pl.isOnline()) return;

            pl.getWorld().playSound(pl.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);

            pl.setVelocity(new Vector(0, 0, 0));
            pl.setFallDistance(0f);
            pl.teleport(target);

            playTeleportAnimation(pl.getLocation(), override != null ? override : Particle.END_ROD, style != null ? style : "SPIRAL");
            pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            pl.sendMessage(plugin.getLang().s("info.teleported").replace("%reason%", reason));
            pl.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));
        }, total * 20L);
        pendingTeleports.put(id, tpTask);
    }

    public void cancelTeleport(UUID id, boolean notify) {
        BukkitTask t = pendingTeleports.remove(id); if (t != null) t.cancel();
        BukkitTask c = pendingCountdowns.remove(id); if (c != null) c.cancel();
        pendingStartLoc.remove(id);

        if (notify) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.sendMessage(plugin.getLang().s("info.teleport-cancelled"));
        }
    }

    public void cancelAllTeleports() {
        pendingTeleports.values().forEach(BukkitTask::cancel);
        pendingCountdowns.values().forEach(BukkitTask::cancel);
    }

    public boolean hasMoved(Player p) {
        Location start = pendingStartLoc.get(p.getUniqueId());
        if (start == null) return false;
        Location to = p.getLocation();
        return start.getWorld() != null && start.getWorld().equals(to.getWorld()) &&
                (start.getBlockX() != to.getBlockX() || start.getBlockY() != to.getBlockY() || start.getBlockZ() != to.getBlockZ());
    }

    private void playTeleportAnimation(Location center, Particle particle, String style) {
        if (center.getWorld() == null) return;
        if ("PULSE".equalsIgnoreCase(style)) {
            center.getWorld().spawnParticle(particle, center.clone().add(0, 1, 0), 80, .8, .5, .8, .02);
            return;
        }
        for (int i = 0; i < 40; i++) {
            double t = i / 10.0;
            double y = 0.2 * t;
            double r = 0.6;
            double x = Math.cos(t * 2) * r;
            double z = Math.sin(t * 2) * r;
            center.getWorld().spawnParticle(particle, center.clone().add(x, 1 + y, z), 2, 0, 0, 0, 0);
        }
    }
}