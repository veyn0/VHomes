package dev.veyno.vHomes.home;

import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HomeCache {

    private final Plugin p;
    private Set<HomeDTO> homes;


    public HomeCache(Plugin p){
        this.p = p;
    }

    public void setHomes(Set<HomeDTO> homes) {
        this.homes = homes;
    }

    public Set<HomeDTO> getHomes() {
        return homes;
    }

    public HomeDTO getHome(UUID homeId){
        for(HomeDTO home : homes){
            if(home.homeId().equals(homeId)) return home;
        }
        return null;
    }

    public Set<HomeDTO> getHomesByPlayer(UUID playerId){
        Set<HomeDTO> result = new HashSet<>();
        for(HomeDTO home : homes){
            if(home.ownerId().equals(playerId)) result.add(home);
        }
        return result.isEmpty() ? null : result;
    }
}
