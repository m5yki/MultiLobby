package me.maykitron.multiLobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class RtpQueueManager {
    private final MultiLobby plugin;
    private final Queue<RtpRequest> queue = new LinkedList<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private boolean processing = false;

    public RtpQueueManager(MultiLobby plugin) {
        this.plugin = plugin;
    }

    public void addToQueue(Player p, String world) {
        UUID id = p.getUniqueId();

        // 1. COOLDOWN (BEKLEME SÜRESİ) KONTROLÜ
        if (cooldowns.containsKey(id)) {
            long timeLeft = cooldowns.get(id) - System.currentTimeMillis();
            if (timeLeft > 0) {
                // Kalan süreyi dakika ve saniyeye çevir
                int seconds = (int) (timeLeft / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                String timeString = (minutes > 0 ? minutes + " dakika " : "") + seconds + " saniye";
                p.sendMessage(plugin.getLang().s("rtp.on-cooldown", "§cKaptan yorgun! Tekrar ışınlanmak için %time% beklemelisin.").replace("%time%", timeString));
                return; // Sıraya almadan işlemi iptal et
            }
        }

        // 2. ZATEN SIRADA MI KONTROLÜ
        if (isInQueue(id)) {
            p.sendMessage(plugin.getLang().s("rtp.already-in-queue", "§cZaten Kaptan'ın listesindesin!"));
            return;
        }

        // 3. SIRAYA EKLE
        queue.add(new RtpRequest(id, world));
        String msg = plugin.getLang().s("rtp.queue-joined", "§bKaptan seni sıraya aldı. Önündeki kişi: §e%pos%").replace("%pos%", String.valueOf(queue.size()));
        p.sendMessage(msg);

        if (!processing) processNext();
    }

    private void processNext() {
        if (queue.isEmpty()) {
            processing = false;
            return;
        }

        processing = true;
        RtpRequest request = queue.poll();
        Player p = Bukkit.getPlayer(request.uuid);

        if (p != null && p.isOnline()) {
            p.sendMessage(plugin.getLang().s("rtp.teleporting", "§aKaptan seni fırlatıyor! Işınlanıyorsun..."));

            // Başarılı şekilde işleme alınan oyuncuya Cooldown tanımla
            int cdSeconds = plugin.getConfig().getInt("rtp.cooldown-seconds", 600); // Varsayılan 10 dakika (600 saniye)
            cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (cdSeconds * 1000L));

            // BetterRTP'yi konsoldan tetikle
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "betterrtp:rtp player " + p.getName() + " " + request.world);
        }

        // Bir sonraki kişi için 5 saniye bekle
        Bukkit.getScheduler().runTaskLater(plugin, this::processNext, 100L);
    }

    public boolean isInQueue(UUID uuid) {
        return queue.stream().anyMatch(r -> r.uuid.equals(uuid));
    }

    private static class RtpRequest {
        UUID uuid; String world;
        RtpRequest(UUID uuid, String world) { this.uuid = uuid; this.world = world; }
    }
}