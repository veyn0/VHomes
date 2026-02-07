package dev.veyno.vHomes;

import dev.veyno.vHomes.home.HomeCache;
import dev.veyno.vHomes.home.HomeRepository;
import dev.veyno.vHomes.home.HomeService;
import dev.veyno.vHomes.util.ClickableInventoryV4;
import dev.veyno.vHomes.util.Database;
import dev.veyno.vHomes.util.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class VHomes extends JavaPlugin {

    private Database homesDb = new Database("homes", 5, "plugins/VHomes/homes.sqlite");

    private HomeService homeService;
    private HomeRepository homeRepository;
    private HomeCache homeCache;
    private HomeManager homeManager;
    private TeleportManager tpManager;

    private ClickableInventoryV4.InventoryManager inventoryManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        homeService = new HomeService(this);
        homeRepository = new HomeRepository(this);
        homeCache = new HomeCache(this);
        homeManager = new HomeManager(this);
        inventoryManager = new ClickableInventoryV4.InventoryManager(this);
        tpManager = new TeleportManager(this);

        if(!getConfig().getString("database-connection", "local").equalsIgnoreCase("local")){
            Bukkit.getLogger().info("[VHomes] using MySQL as Database...");
            homesDb = new Database("homes",getConfig().getString("database-connection"), 5 );
        }
        else{
            Bukkit.getLogger().info("[VHomes] using SQLite as Database...");
        }

        homeService.startCacheUpdateSchedule(getConfig().getInt("update-interval", 300));
        homeService.startPendingUpdateSchedule(getConfig().getInt("save-interval", 5));

        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("home").setTabCompleter(new HomeCommand(this));

        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("sethome").setTabCompleter(new SetHomeCommand(this));
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Database getHomesDb() {
        return homesDb;
    }

    public HomeCache getHomeCache() {
        return homeCache;
    }

    public HomeRepository getHomeRepository() {
        return homeRepository;
    }

    public HomeService getHomeService() {
        return homeService;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public ClickableInventoryV4.InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public TeleportManager getTpManager() {
        return tpManager;
    }
}
