package me.maykitron.multiLobby;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackManager implements Listener {

    private final MultiLobby plugin;
    private final Map<UUID, Location> lastDeathLocations = new HashMap<>();

    public BackManager(MultiLobby plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        // Eğer katil bir oyuncu DEĞİLSE (doğal sebeplerse) konumu kaydet
        if (p.getKiller() == null) {
            lastDeathLocations.put(p.getUniqueId(), p.getLocation());
            p.sendMessage(plugin.getLang().s("back.location-saved", "§7Ölüm noktanız kaydedildi. §a/back §7yazarak dönebilirsiniz."));
        }
    }

    public void teleportBack(Player p) {
        UUID id = p.getUniqueId();
        if (!lastDeathLocations.containsKey(id)) {
            p.sendMessage(plugin.getLang().s("back.no-location", "§cDönebileceğin bir ölüm noktası bulunamadı!"));
            return;
        }

        Location target = lastDeathLocations.get(id);

        // Mevcut TeleportUtil'i kullanarak ışınla
        plugin.getTeleportUtil().startWarmupTeleport(p, target, plugin.getLang().s("back.name", "Ölüm Noktası"), null, "PULSE");

        // Işınlanma tamamlandığında efektleri ver (Bu işlem TeleportUtil içinden de tetiklenebilir veya direkt verilebilir)
        // Burada basitlik için direkt veriyoruz, TeleportUtil bittiğinde verilmesini istersen TeleportUtil'e callback ekleyebiliriz.
        applyBackEffects(p);

        // Konumu siliyoruz (tek kullanımlık olması için)
        lastDeathLocations.remove(id);
    }

    public void applyBackEffects(Player p) {
        // 30 saniye = 600 tick
        int duration = 600;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, duration, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 0)); // Yenilenme 1
    }
}