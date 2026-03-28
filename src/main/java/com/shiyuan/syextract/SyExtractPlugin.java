package com.shiyuan.syextract;

import com.shiyuan.syextract.command.SyExtractCommand;
import com.shiyuan.syextract.gui.GUIManager;
import com.shiyuan.syextract.manager.BanManager;
import com.shiyuan.syextract.manager.EconomyManager;
import com.shiyuan.syextract.manager.RedEnvelopeManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SyExtractPlugin extends JavaPlugin {

    private static SyExtractPlugin instance;
    private EconomyManager economyManager;
    private RedEnvelopeManager redEnvelopeManager;
    private BanManager banManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        reloadConfig();
        
        if (!setupEconomy()) {
            getLogger().severe("未找到Vault插件! 插件将禁用...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.economyManager = new EconomyManager();
        this.redEnvelopeManager = new RedEnvelopeManager(this);
        this.banManager = new BanManager(this);
        this.guiManager = new GUIManager(this);
        
        getCommand("syextract").setExecutor(new SyExtractCommand(this));
        getCommand("syextract").setTabCompleter(new SyExtractCommand(this));
        
        getServer().getPluginManager().registerEvents(guiManager, this);
        
        redEnvelopeManager.loadEnvelopes();
        banManager.loadBans();
        
        getLogger().info("SyExtract 插件已启用!");
        getLogger().info("作者: Shiyuan");
        getLogger().info("年份: 2026");
    }

    @Override
    public void onDisable() {
        if (redEnvelopeManager != null) {
            redEnvelopeManager.saveEnvelopes();
        }
        if (banManager != null) {
            banManager.saveBans();
        }
        getLogger().info("SyExtract 插件已禁用!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        return true;
    }

    public static SyExtractPlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public RedEnvelopeManager getRedEnvelopeManager() {
        return redEnvelopeManager;
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        redEnvelopeManager.loadEnvelopes();
        banManager.loadBans();
    }
}
