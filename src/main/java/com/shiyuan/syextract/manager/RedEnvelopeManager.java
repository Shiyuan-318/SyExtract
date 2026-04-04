package com.shiyuan.syextract.manager;

import com.shiyuan.syextract.SyExtractPlugin;
import com.shiyuan.syextract.model.RedEnvelope;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RedEnvelopeManager {

    private final SyExtractPlugin plugin;
    private final Map<UUID, RedEnvelope> envelopes;
    private final Map<String, RedEnvelope> shortIdMap;
    private final File dataFile;

    public RedEnvelopeManager(SyExtractPlugin plugin) {
        this.plugin = plugin;
        this.envelopes = new HashMap<>();
        this.shortIdMap = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "envelopes.yml");
    }

    public RedEnvelope createEnvelope(UUID sender, String senderName, String name, double amount, int count) {
        long expireHours = plugin.getConfig().getLong("red-envelope.default-expire-hours", 24);
        RedEnvelope envelope = new RedEnvelope(sender, senderName, name, amount, count, expireHours);
        envelopes.put(envelope.getId(), envelope);
        shortIdMap.put(envelope.getShortId(), envelope);
        saveEnvelopes();
        return envelope;
    }

    public RedEnvelope getEnvelope(UUID id) {
        RedEnvelope envelope = envelopes.get(id);
        if (envelope != null && (envelope.isExpired() || envelope.isFullyClaimed())) {
            if (envelope.isExpired() && !envelope.isRefunded() && !envelope.isFullyClaimed()) {
                refundEnvelope(envelope);
            }
            envelopes.remove(id);
            shortIdMap.remove(envelope.getShortId());
            return null;
        }
        return envelope;
    }

    public RedEnvelope getEnvelopeByShortId(String shortId) {
        RedEnvelope envelope = shortIdMap.get(shortId.toUpperCase());
        if (envelope != null) {
            if (envelope.isExpired() || envelope.isFullyClaimed()) {
                if (envelope.isExpired() && !envelope.isRefunded() && !envelope.isFullyClaimed()) {
                    refundEnvelope(envelope);
                }
                envelopes.remove(envelope.getId());
                shortIdMap.remove(envelope.getShortId());
                return null;
            }
        }
        return envelope;
    }

    public void refundEnvelope(RedEnvelope envelope) {
        if (envelope.isRefunded()) {
            return;
        }
        
        double unclaimedAmount = envelope.getUnclaimedAmount();
        if (unclaimedAmount > 0) {
            plugin.getEconomyManager().deposit(envelope.getSender(), unclaimedAmount);
            envelope.setRefunded(true);
            
            org.bukkit.entity.Player sender = plugin.getServer().getPlayer(envelope.getSender());
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(com.shiyuan.syextract.util.MessageUtil.getMessage("info.refund", 
                    Map.of("amount", String.format("%.2f", unclaimedAmount), "name", envelope.getName())));
            }
        }
    }

    public List<RedEnvelope> getAvailableEnvelopes() {
        cleanupExpiredEnvelopes();
        return new ArrayList<>(envelopes.values());
    }

    public List<RedEnvelope> getAvailableEnvelopesForPlayer(UUID playerId) {
        cleanupExpiredEnvelopes();
        List<RedEnvelope> available = new ArrayList<>();
        for (RedEnvelope envelope : envelopes.values()) {
            if (!envelope.hasClaimed(playerId) && !envelope.isExpired() && !envelope.isFullyClaimed()) {
                available.add(envelope);
            }
        }
        return available;
    }

    public double claimEnvelope(UUID envelopeId, UUID playerId) {
        RedEnvelope envelope = getEnvelope(envelopeId);
        if (envelope == null) {
            return -1;
        }
        
        double amount = envelope.claim(playerId);
        if (amount > 0) {
            saveEnvelopes();
            if (envelope.isFullyClaimed()) {
                envelopes.remove(envelopeId);
                shortIdMap.remove(envelope.getShortId());
                broadcastEnvelopeCompleted(envelope);
            }
        }
        return amount;
    }

    public double claimEnvelopeByShortId(String shortId, UUID playerId) {
        RedEnvelope envelope = getEnvelopeByShortId(shortId);
        if (envelope == null) {
            return -1;
        }

        double amount = envelope.claim(playerId);
        if (amount > 0) {
            saveEnvelopes();
            if (envelope.isFullyClaimed()) {
                envelopes.remove(envelope.getId());
                shortIdMap.remove(envelope.getShortId());
                broadcastEnvelopeCompleted(envelope);
            }
        }
        return amount;
    }

    private void broadcastEnvelopeCompleted(RedEnvelope envelope) {
        RedEnvelope.LuckiestPlayerInfo luckiest = envelope.getLuckiestPlayer();
        if (luckiest == null) {
            return;
        }

        String luckiestPlayerName = getPlayerName(luckiest.getPlayerId());
        String message = com.shiyuan.syextract.util.MessageUtil.getMessage("broadcast.envelope-completed",
            Map.of("name", envelope.getName(),
                   "player", luckiestPlayerName,
                   "amount", String.format("%.2f", luckiest.getAmount())),
            "&6&l🎉 红包通知 &r&7标题为 &e{name} &7的红包被领完了，&6{player} &7是手气王，领到了 &e{amount} &7金币！");

        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private String getPlayerName(UUID playerId) {
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            return player.getName();
        }

        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerId);
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }

    public void cleanupExpiredEnvelopes() {
        Iterator<Map.Entry<UUID, RedEnvelope>> iterator = envelopes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RedEnvelope> entry = iterator.next();
            RedEnvelope envelope = entry.getValue();
            if (envelope.isExpired() || envelope.isFullyClaimed()) {
                if (envelope.isExpired() && !envelope.isRefunded() && !envelope.isFullyClaimed()) {
                    refundEnvelope(envelope);
                }
                shortIdMap.remove(envelope.getShortId());
                iterator.remove();
            }
        }
    }

    public void loadEnvelopes() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("envelopes");

        if (section == null) {
            return;
        }

        envelopes.clear();
        shortIdMap.clear();

        int maxId = 0;

        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection envSection = section.getConfigurationSection(key);
                if (envSection == null) continue;

                UUID id = UUID.fromString(key);
                UUID sender = UUID.fromString(envSection.getString("sender"));
                String senderName = envSection.getString("senderName", "Unknown");
                String name = envSection.getString("name", "红包");
                double totalAmount = envSection.getDouble("totalAmount");
                int totalCount = envSection.getInt("totalCount");
                long expireTime = envSection.getLong("expireTime");
                boolean refunded = envSection.getBoolean("refunded", false);
                int numericId = envSection.getInt("numericId", 0);

                if (System.currentTimeMillis() > expireTime) {
                    continue;
                }

                long expireHours = (expireTime - System.currentTimeMillis()) / (60 * 60 * 1000) + 1;
                RedEnvelope envelope = new RedEnvelope(sender, senderName, name, totalAmount, totalCount, expireHours);

                if (refunded) {
                    envelope.setRefunded(true);
                }

                List<String> claimedPlayers = envSection.getStringList("claimedPlayers");
                for (String playerUuid : claimedPlayers) {
                    envelope.claim(UUID.fromString(playerUuid));
                }

                if (!envelope.isFullyClaimed() && !envelope.isExpired()) {
                    envelopes.put(id, envelope);
                    shortIdMap.put(envelope.getShortId(), envelope);
                    if (numericId > maxId) {
                        maxId = numericId;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载红包数据失败: " + key);
            }
        }

        // 设置下一个ID为最大ID+1
        if (maxId > 0) {
            RedEnvelope.setNextId(maxId + 1);
        }
    }

    public void saveEnvelopes() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            FileConfiguration config = new YamlConfiguration();
            ConfigurationSection section = config.createSection("envelopes");

            for (Map.Entry<UUID, RedEnvelope> entry : envelopes.entrySet()) {
                RedEnvelope envelope = entry.getValue();
                if (envelope.isExpired() || envelope.isFullyClaimed()) {
                    continue;
                }

                ConfigurationSection envSection = section.createSection(entry.getKey().toString());
                envSection.set("sender", envelope.getSender().toString());
                envSection.set("senderName", envelope.getSenderName());
                envSection.set("name", envelope.getName());
                envSection.set("totalAmount", envelope.getTotalAmount());
                envSection.set("totalCount", envelope.getTotalCount());
                envSection.set("expireTime", envelope.getExpireTime());
                envSection.set("refunded", envelope.isRefunded());
                envSection.set("numericId", envelope.getNumericId());
                
                List<String> claimedPlayers = new ArrayList<>();
                for (UUID playerId : envelope.getClaimedPlayers()) {
                    claimedPlayers.add(playerId.toString());
                }
                envSection.set("claimedPlayers", claimedPlayers);
            }

            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存红包数据失败: " + e.getMessage());
        }
    }
}
