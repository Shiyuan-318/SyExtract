package com.shiyuan.syextract.util;

import com.shiyuan.syextract.SyExtractPlugin;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageUtil {

    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String getMessage(String path) {
        String prefix = SyExtractPlugin.getInstance().getConfig().getString("messages.prefix", "&8[&cSyExtract&8] &r");
        String message = SyExtractPlugin.getInstance().getConfig().getString("messages." + path, "&c消息未找到: " + path);
        return colorize(prefix + message);
    }

    public static String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public static List<String> getMessageList(String path) {
        List<String> messages = SyExtractPlugin.getInstance().getConfig().getStringList("messages." + path);
        return messages.stream()
                .map(MessageUtil::colorize)
                .collect(Collectors.toList());
    }

    public static void sendMessage(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(getMessage(path));
    }

    public static void sendMessage(org.bukkit.command.CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(path, placeholders));
    }
}
