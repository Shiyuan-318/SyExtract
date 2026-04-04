package com.shiyuan.syextract.manager;

import com.shiyuan.syextract.SyExtractPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LanguageManager {

    private final SyExtractPlugin plugin;
    private FileConfiguration langConfig;
    private String currentLanguage;

    public LanguageManager(SyExtractPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        this.currentLanguage = plugin.getConfig().getString("language", "zh");
        
        // 确保语言文件存在
        saveDefaultLanguageFiles();
        
        // 加载语言文件
        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLanguage + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + currentLanguage + ".yml not found, falling back to zh");
            langFile = new File(plugin.getDataFolder(), "lang/zh.yml");
            this.currentLanguage = "zh";
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 从jar加载默认语言作为fallback
        InputStream defaultStream = plugin.getResource("lang/" + currentLanguage + ".yml");
        if (defaultStream != null) {
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defaultConfig);
        }
    }

    private void saveDefaultLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // 保存默认语言文件
        saveLanguageFile("zh.yml");
        saveLanguageFile("en.yml");
    }

    private void saveLanguageFile(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("lang/" + fileName)) {
                if (in != null) {
                    java.nio.file.Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save language file: " + fileName);
            }
        }
    }

    public void reloadLanguage() {
        loadLanguage();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
        plugin.getConfig().set("language", language);
        plugin.saveConfig();
        reloadLanguage();
    }

    public String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path) {
        String prefix = langConfig.getString("prefix", "&8[&cSyExtract&8] &r");
        String message = langConfig.getString(path, "&cMessage not found: " + path);
        return colorize(prefix + message);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String getMessage(String path, Map<String, String> placeholders, String defaultMessage) {
        String prefix = langConfig.getString("prefix", "&8[&cSyExtract&8] &r");
        String message = langConfig.getString(path, defaultMessage);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return colorize(prefix + message);
    }

    public String getRawMessage(String path) {
        return langConfig.getString(path, "");
    }

    public String getRawMessage(String path, String defaultValue) {
        return langConfig.getString(path, defaultValue);
    }

    public List<String> getMessageList(String path) {
        List<String> messages = langConfig.getStringList(path);
        return messages.stream()
                .map(this::colorize)
                .collect(Collectors.toList());
    }

    public List<String> getRawMessageList(String path) {
        return langConfig.getStringList(path);
    }
}
