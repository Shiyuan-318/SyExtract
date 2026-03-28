package com.shiyuan.syextract.manager;

import com.shiyuan.syextract.SyExtractPlugin;
import com.shiyuan.syextract.model.PlayerBan;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BanManager {

    private final SyExtractPlugin plugin;
    private final Map<UUID, PlayerBan> bans;
    private final File dataFile;

    public BanManager(SyExtractPlugin plugin) {
        this.plugin = plugin;
        this.bans = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "bans.yml");
    }

    public void banPlayer(UUID playerId, String playerName, long durationHours) {
        PlayerBan ban = new PlayerBan(playerId, playerName, durationHours, true, true);
        bans.put(playerId, ban);
        saveBans();
    }

    public void unbanPlayer(UUID playerId) {
        bans.remove(playerId);
        saveBans();
    }

    public boolean isBanned(UUID playerId) {
        cleanupExpiredBans();
        PlayerBan ban = bans.get(playerId);
        return ban != null && !ban.isExpired();
    }

    public boolean canCreateEnvelope(UUID playerId) {
        cleanupExpiredBans();
        PlayerBan ban = bans.get(playerId);
        return ban == null || ban.canCreate();
    }

    public boolean canClaimEnvelope(UUID playerId) {
        cleanupExpiredBans();
        PlayerBan ban = bans.get(playerId);
        return ban == null || ban.canClaim();
    }

    public PlayerBan getBan(UUID playerId) {
        cleanupExpiredBans();
        return bans.get(playerId);
    }

    public void cleanupExpiredBans() {
        Iterator<Map.Entry<UUID, PlayerBan>> iterator = bans.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerBan> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    public void loadBans() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("bans");
        
        if (section == null) {
            return;
        }

        bans.clear();
        
        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection banSection = section.getConfigurationSection(key);
                if (banSection == null) continue;

                UUID playerId = UUID.fromString(key);
                String playerName = banSection.getString("playerName", "Unknown");
                long expireTime = banSection.getLong("expireTime");
                boolean banCreate = banSection.getBoolean("banCreate", true);
                boolean banClaim = banSection.getBoolean("banClaim", true);
                
                if (System.currentTimeMillis() > expireTime) {
                    continue;
                }

                long remainingHours = (expireTime - System.currentTimeMillis()) / (60 * 60 * 1000) + 1;
                PlayerBan ban = new PlayerBan(playerId, playerName, remainingHours, banCreate, banClaim);
                bans.put(playerId, ban);
            } catch (Exception e) {
                plugin.getLogger().warning("加载封禁数据失败: " + key);
            }
        }
    }

    public void saveBans() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            FileConfiguration config = new YamlConfiguration();
            ConfigurationSection section = config.createSection("bans");

            for (Map.Entry<UUID, PlayerBan> entry : bans.entrySet()) {
                PlayerBan ban = entry.getValue();
                if (ban.isExpired()) {
                    continue;
                }

                ConfigurationSection banSection = section.createSection(entry.getKey().toString());
                banSection.set("playerName", ban.getPlayerName());
                banSection.set("banTime", ban.getBanTime());
                banSection.set("expireTime", ban.getExpireTime());
                banSection.set("banCreate", ban.isBanCreate());
                banSection.set("banClaim", ban.isBanClaim());
            }

            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存封禁数据失败: " + e.getMessage());
        }
    }
}
