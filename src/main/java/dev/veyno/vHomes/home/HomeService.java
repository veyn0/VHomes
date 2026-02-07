package dev.veyno.vHomes.home;

import dev.veyno.vHomes.VHomes;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HomeService {

    private Set<HomeDTO> pendingSaves = new HashSet<>();

    private final VHomes plugin;

    public HomeService(VHomes plugin){
        this.plugin = plugin;
    }

    public void startCacheUpdateSchedule(int seconds){
        Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> queueChanges(),
                1, seconds, TimeUnit.SECONDS
        );
    }

    public void startPendingUpdateSchedule(int seconds){
        Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> updatePending(),
                1, seconds, TimeUnit.SECONDS
        );
    }

    private void updatePending(){
        Set<HomeDTO> updates = new HashSet<>(pendingSaves);
        pendingSaves.clear();
        for(HomeDTO home : updates){
            plugin.getHomeRepository().saveHome(home);
            plugin.getHomeCache().getHomes().add(home);
        }
    }

    private void queueChanges(){
        plugin.getHomeCache().setHomes(plugin.getHomeRepository().getHomes());
    }

    public void queueChanges(HomeDTO home){
        pendingSaves.add(home);
    }




}
