package com.shiyuan.syextract.command;

import com.shiyuan.syextract.SyExtractPlugin;
import com.shiyuan.syextract.model.PlayerBan;
import com.shiyuan.syextract.model.RedEnvelope;
import com.shiyuan.syextract.util.MessageUtil;
import com.shiyuan.syextract.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class SyExtractCommand implements CommandExecutor, TabCompleter {

    private final SyExtractPlugin plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SyExtractCommand(SyExtractPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create", "c" -> handleCreate(sender, args);
            case "open", "o" -> handleOpen(sender);
            case "ban", "b" -> handleBan(sender, args);
            case "unban", "u" -> handleUnban(sender, args);
            case "reload", "r" -> handleReload(sender);
            case "broadcast", "br" -> handleBroadcast(sender, args);
            case "claim", "cl" -> handleClaim(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (!sender.hasPermission("syextract.create")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (!plugin.getBanManager().canCreateEnvelope(player.getUniqueId())) {
            PlayerBan ban = plugin.getBanManager().getBan(player.getUniqueId());
            sender.sendMessage(MessageUtil.getMessage("error.banned-create", 
                Map.of("expire", DATE_FORMAT.format(new Date(ban.getExpireTime())))));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(MessageUtil.getMessage("command.usage-create"));
            return;
        }

        String name = args[1];
        double amount;
        int count;

        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                sender.sendMessage(MessageUtil.getMessage("error.invalid-amount"));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage("error.invalid-amount"));
            return;
        }

        try {
            count = Integer.parseInt(args[3]);
            int minCount = plugin.getConfig().getInt("red-envelope.min-count", 1);
            int maxCount = plugin.getConfig().getInt("red-envelope.max-count", 100);
            if (count < minCount || count > maxCount) {
                sender.sendMessage(MessageUtil.getMessage("error.invalid-count", 
                    Map.of("min", String.valueOf(minCount), "max", String.valueOf(maxCount))));
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.getMessage("error.invalid-count", 
                Map.of("min", "1", "max", "100")));
            return;
        }

        double minAmount = plugin.getConfig().getDouble("red-envelope.min-amount", 1.0);
        if (amount < minAmount) {
            sender.sendMessage(MessageUtil.getMessage("error.invalid-amount"));
            return;
        }

        double fee = calculateFee(amount);
        double totalCost = amount + fee;

        if (!plugin.getEconomyManager().hasEnough(player.getUniqueId(), totalCost)) {
            sender.sendMessage(MessageUtil.getMessage("error.insufficient-balance", 
                Map.of("amount", String.format("%.2f", totalCost))));
            return;
        }

        if (!plugin.getEconomyManager().withdraw(player.getUniqueId(), totalCost)) {
            sender.sendMessage(MessageUtil.getMessage("error.insufficient-balance", 
                Map.of("amount", String.format("%.2f", totalCost))));
            return;
        }

        RedEnvelope envelope = plugin.getRedEnvelopeManager().createEnvelope(
            player.getUniqueId(), 
            player.getName(), 
            name, 
            amount, 
            count
        );

        sender.sendMessage(MessageUtil.getMessage("success.create", 
            Map.of("amount", String.format("%.2f", amount), 
                   "fee", String.format("%.2f", fee),
                   "total", String.format("%.2f", totalCost),
                   "id", envelope.getShortId())));

        broadcastEnvelope(envelope);
    }

    private void broadcastEnvelope(RedEnvelope envelope) {
        String broadcastFormat = MessageUtil.getRawMessage("broadcast.format", 
            "&6&l🧧 Red Envelope! &e{sender} &7sent &6&l[{name}] &7Total: &a{amount} &7coins");

        String clickText = MessageUtil.getRawMessage("broadcast.click-text", "&a&l[Claim Now]");
        String hoverText = MessageUtil.getRawMessage("broadcast.hover-text", "&eClick to claim!");

        String message = broadcastFormat
            .replace("{sender}", envelope.getSenderName())
            .replace("{name}", envelope.getName())
            .replace("{amount}", String.format("%.2f", envelope.getTotalAmount()))
            .replace("{id}", envelope.getShortId());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(MessageUtil.colorize(message));
            player.sendMessage(MessageUtil.colorize(clickText) + " " + MessageUtil.colorize("&7(Click to run: &e/sye claim " + envelope.getShortId() + "&7)"));
        }
    }

    private void handleOpen(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (!sender.hasPermission("syextract.open")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        plugin.getGuiManager().openGUI(player);
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("syextract.ban")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtil.getMessage("command.usage-ban"));
            sender.sendMessage(MessageUtil.getMessage("command.usage-ban-time"));
            return;
        }

        String playerName = args[1];
        String timeStr = args[2];

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || (!target.hasPlayedBefore() && target.getUniqueId() == null)) {
            sender.sendMessage(MessageUtil.getMessage("error.player-not-found"));
            return;
        }

        long hours = TimeUtil.parseTime(timeStr);
        if (hours <= 0) {
            sender.sendMessage(MessageUtil.getMessage("error.invalid-time"));
            return;
        }

        plugin.getBanManager().banPlayer(target.getUniqueId(), playerName, hours);
        
        long expireTime = System.currentTimeMillis() + (hours * 60 * 60 * 1000);
        sender.sendMessage(MessageUtil.getMessage("success.ban", 
            Map.of("player", playerName, "expire", DATE_FORMAT.format(new Date(expireTime)))));
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("syextract.unban")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage("command.usage-unban"));
            return;
        }

        String playerName = args[1];

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null) {
            sender.sendMessage(MessageUtil.getMessage("error.player-not-found"));
            return;
        }

        plugin.getBanManager().unbanPlayer(target.getUniqueId());
        sender.sendMessage(MessageUtil.getMessage("success.unban", Map.of("player", playerName)));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("syextract.reload")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(MessageUtil.getMessage("success.reload"));
    }

    private void handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("syextract.broadcast")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage("command.usage-broadcast"));
            return;
        }

        String shortId = args[1].toUpperCase();
        RedEnvelope envelope = plugin.getRedEnvelopeManager().getEnvelopeByShortId(shortId);

        if (envelope == null) {
            sender.sendMessage(MessageUtil.getMessage("error.envelope-not-found"));
            return;
        }

        String broadcastFormat = MessageUtil.getRawMessage("broadcast.format", 
            "&6&l🧧 Red Envelope! &e{sender} &7sent &6&l[{name}] &7Total: &a{amount} &7coins");

        String clickText = MessageUtil.getRawMessage("broadcast.click-text", "&a&l[Claim Now]");
        String hoverText = MessageUtil.getRawMessage("broadcast.hover-text", "&eClick to claim!");

        String message = broadcastFormat
            .replace("{sender}", envelope.getSenderName())
            .replace("{name}", envelope.getName())
            .replace("{amount}", String.format("%.2f", envelope.getTotalAmount()))
            .replace("{id}", envelope.getShortId());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(MessageUtil.colorize(message));
            player.sendMessage(MessageUtil.colorize(clickText) + " " + MessageUtil.colorize("&7(Click to run: &e/sye claim " + envelope.getShortId() + "&7)"));
        }

        sender.sendMessage(MessageUtil.getMessage("success.broadcast",
            Map.of("id", envelope.getShortId())));
    }

    private void handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (!sender.hasPermission("syextract.claim")) {
            sender.sendMessage(MessageUtil.getMessage("error.no-permission"));
            return;
        }

        if (!plugin.getBanManager().canClaimEnvelope(player.getUniqueId())) {
            PlayerBan ban = plugin.getBanManager().getBan(player.getUniqueId());
            sender.sendMessage(MessageUtil.getMessage("error.banned-claim", 
                Map.of("expire", DATE_FORMAT.format(new Date(ban.getExpireTime())))));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.getMessage("command.usage-claim"));
            return;
        }

        String shortId = args[1].toUpperCase();
        double amount = plugin.getRedEnvelopeManager().claimEnvelopeByShortId(shortId, player.getUniqueId());

        if (amount > 0) {
            RedEnvelope envelope = plugin.getRedEnvelopeManager().getEnvelopeByShortId(shortId);
            String senderName = envelope != null ? envelope.getSenderName() : "Unknown";
            
            plugin.getEconomyManager().deposit(player.getUniqueId(), amount);
            
            player.sendMessage(MessageUtil.getMessage("success.claim", 
                Map.of("sender", senderName, "amount", String.format("%.2f", amount))));
        } else {
            player.sendMessage(MessageUtil.getMessage("error.envelope-not-found"));
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpMessages = MessageUtil.getMessageList("info.help");
        for (String message : helpMessages) {
            sender.sendMessage(message);
        }
    }

    private double calculateFee(double amount) {
        if (!plugin.getConfig().getBoolean("fee.enabled", true)) {
            return 0;
        }

        double percentage = plugin.getConfig().getDouble("fee.percentage", 0.05);
        double minFee = plugin.getConfig().getDouble("fee.min-fee", 1.0);
        
        double fee = amount * percentage;
        fee = Math.round(fee * 100.0) / 100.0;
        
        return Math.max(fee, minFee);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("syextract.create")) subCommands.add("create");
            if (sender.hasPermission("syextract.open")) subCommands.add("open");
            if (sender.hasPermission("syextract.ban")) subCommands.add("ban");
            if (sender.hasPermission("syextract.unban")) subCommands.add("unban");
            if (sender.hasPermission("syextract.reload")) subCommands.add("reload");
            if (sender.hasPermission("syextract.broadcast")) subCommands.add("broadcast");
            if (sender.hasPermission("syextract.claim")) subCommands.add("claim");
            
            String partial = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ((subCommand.equals("ban") && sender.hasPermission("syextract.ban")) ||
                (subCommand.equals("unban") && sender.hasPermission("syextract.unban"))) {
                
                String partial = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            } else if (subCommand.equals("broadcast") && sender.hasPermission("syextract.broadcast")) {
                for (RedEnvelope envelope : plugin.getRedEnvelopeManager().getAvailableEnvelopes()) {
                    completions.add(envelope.getShortId());
                }
            } else if (subCommand.equals("claim") && sender.hasPermission("syextract.claim")) {
                if (sender instanceof Player player) {
                    for (RedEnvelope envelope : plugin.getRedEnvelopeManager().getAvailableEnvelopesForPlayer(player.getUniqueId())) {
                        completions.add(envelope.getShortId());
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("ban") && sender.hasPermission("syextract.ban")) {
                completions.addAll(Arrays.asList("1h", "6h", "12h", "1d", "3d", "7d", "30d"));
            }
        }

        return completions;
    }
}

