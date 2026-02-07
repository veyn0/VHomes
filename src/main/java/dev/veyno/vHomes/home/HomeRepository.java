package dev.veyno.vHomes.home;

import dev.veyno.vHomes.VHomes;
import dev.veyno.vHomes.util.Database;
import dev.veyno.vHomes.util.LocalDatetimeUtil;
import dev.veyno.vHomes.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HomeRepository {

    private final VHomes plugin;

    public HomeRepository(VHomes plugin){
        this.plugin = plugin;
    }

    public Set<HomeDTO> getHomes(){
        Set<HomeDTO> result = new HashSet<>();
        Set<UUID> homeIds = getHomeIds();
        if(homeIds==null) return null;
        for(UUID id : homeIds ){
            HomeDTO h = getHome(id);
            if(h!=null) result.add(h);
        }
        return result;
    }

    /*

    Table: homeID: UUID - ownerID: UUID - location : String (formatted) - displayname: String - updatedAt: String (formatted)

     */

    public HomeDTO getHome(UUID homeId){
        Database db = plugin.getHomesDb();
        try {
            String key = homeId.toString();
            UUID ownerUUID = UUID.fromString(db.getString(key, 0));
            Location l = LocationUtil.locationFromString(db.getString(key, 1));
            String displayName = db.getString(key, 2);
            LocalDateTime updatedAt = LocalDatetimeUtil.fromString(db.getString(key, 3));
            return new HomeDTO(homeId, ownerUUID, l, displayName, updatedAt);
        }catch (Exception e){
            return null;
        }
    }

    public void saveHome(HomeDTO home){
        Database db = plugin.getHomesDb();
        String key = home.homeId().toString();
        db.set(key, 0, home.ownerId().toString());
        db.set(key, 1, LocationUtil.locationToString(home.location()));
        db.set(key, 2, home.displayName());
        db.set(key, 3, LocalDatetimeUtil.fromLocalDateTime(home.updatedAt()));
    }

    public Set<UUID> getHomeIds(){
        Set<UUID> result = new HashSet<>();
        for(String s: plugin.getHomesDb().keySet()){
            try {
                result.add(UUID.fromString(s));
            }
            catch (Exception e){
                Bukkit.getLogger().warning("Found invalid HomeID entry in Database: "+ s);
            }
        }
        return result;
    }

    public void removeHome(UUID homeId){
        plugin.getHomesDb().remove(homeId.toString());
    }

}
