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
        return SyExtractPlugin.getInstance().getLanguageManager().getMessage(path);
    }

    public static String getMessage(String path, Map<String, String> placeholders) {
        return SyExtractPlugin.getInstance().getLanguageManager().getMessage(path, placeholders);
    }

    public static String getMessage(String path, Map<String, String> placeholders, String defaultMessage) {
        return SyExtractPlugin.getInstance().getLanguageManager().getMessage(path, placeholders, defaultMessage);
    }

    public static String getRawMessage(String path) {
        return SyExtractPlugin.getInstance().getLanguageManager().getRawMessage(path);
    }

    public static String getRawMessage(String path, String defaultValue) {
        return SyExtractPlugin.getInstance().getLanguageManager().getRawMessage(path, defaultValue);
    }

    public static List<String> getMessageList(String path) {
        return SyExtractPlugin.getInstance().getLanguageManager().getMessageList(path);
    }

    public static List<String> getRawMessageList(String path) {
        return SyExtractPlugin.getInstance().getLanguageManager().getRawMessageList(path);
    }

    public static void sendMessage(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(getMessage(path));
    }

    public static void sendMessage(org.bukkit.command.CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(path, placeholders));
    }
}
