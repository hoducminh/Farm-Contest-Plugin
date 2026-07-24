package com.farmcontest;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FarmContest extends JavaPlugin {

    private static FarmContest instance;

    private DatabaseManager databaseManager;
    private ContestManager contestManager;
    private ConfigManager configManager;
    private DataManager dataManager;
    private BossBarManager bossBarManager;

    private MutationManager mutationManager;
    private MutationShopGUI mutationShopGUI;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;


        databaseManager = new DatabaseManager(this);

        configManager = new ConfigManager(this);
        getLogger().info(configManager.getConsoleMessage("starting"));

        dataManager = new DataManager(this);
        bossBarManager = new BossBarManager(this);
        contestManager = new ContestManager(this, configManager, dataManager, bossBarManager);

        if (!setupEconomy()) {
            getLogger().warning(configManager.getConsoleMessage("vault-not-found"));
        }


        mutationManager = new MutationManager(this);
        mutationShopGUI = new MutationShopGUI(this, mutationManager, economy);


        FarmContestCommand commandExecutor = new FarmContestCommand(this, contestManager, dataManager, configManager);
        if (getCommand("farmcontest") != null) {
            getCommand("farmcontest").setExecutor(commandExecutor);
        }
        if (getCommand("fc") != null) {
            getCommand("fc").setExecutor(commandExecutor);
        }


        getServer().getPluginManager().registerEvents(
                new FarmListener(this, contestManager, dataManager, configManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this, contestManager, configManager), this);


        getServer().getPluginManager().registerEvents(mutationShopGUI, this);


        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FarmContestExpansion(this).register();
            getLogger().info(configManager.getConsoleMessage("papi-registered"));
        }

        contestManager.startScheduler();

        getLogger().info(configManager.getConsoleMessage("enabled"));
    }

    @Override
    public void onDisable() {
        if (contestManager != null) contestManager.shutdown();
        if (dataManager != null)    dataManager.saveData();
        if (bossBarManager != null) bossBarManager.removeAllBossBars();
        if (databaseManager != null) databaseManager.close();
        if (configManager != null) {
            getLogger().info(configManager.getConsoleMessage("disabled"));
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static FarmContest getInstance() {
        return instance;
    }

    // ── Getters ─────────────────────────────────────────────
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ContestManager  getContestManager()  { return contestManager; }
    public ConfigManager   getConfigManager()   { return configManager; }
    public DataManager     getDataManager()     { return dataManager; }
    public BossBarManager  getBossBarManager()  { return bossBarManager; }
    public MutationManager getMutationManager() { return mutationManager; }
    public MutationShopGUI getMutationShopGUI() { return mutationShopGUI; }
    public Economy         getEconomy()         { return economy; }
}