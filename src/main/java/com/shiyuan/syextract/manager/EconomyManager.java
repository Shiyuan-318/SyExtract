package com.shiyuan.syextract.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyManager {

    private Economy economy;

    public EconomyManager() {
        setupEconomy();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean hasAccount(UUID playerId) {
        if (economy == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return economy.hasAccount(player);
    }

    public double getBalance(UUID playerId) {
        if (economy == null) return 0;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return economy.getBalance(player);
    }

    public boolean hasEnough(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    public boolean withdraw(UUID playerId, double amount) {
        if (economy == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        if (!hasEnough(playerId, amount)) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(UUID playerId, double amount) {
        if (economy == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isEnabled() {
        return economy != null;
    }
}
