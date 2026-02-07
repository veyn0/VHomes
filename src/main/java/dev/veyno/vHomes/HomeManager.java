package dev.veyno.vHomes;

import dev.veyno.vHomes.home.HomeDTO;
import dev.veyno.vHomes.util.ClickableInventoryV4;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HomeManager {

    private final VHomes plugin;

    public HomeManager(VHomes plugin){
        this.plugin = plugin;
    }

    public void openHomeUi(Player p){
        Component title = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("ui.overview-title", "<red>Error! please contact an Admin")
                .replace("{PLAYER_NAME}", p.getName()));
        ClickableInventoryV4 inv = new ClickableInventoryV4(plugin.getInventoryManager(), title, p);
        List<ClickableInventoryV4.ClickableItem> items = new ArrayList<>();
        Set<HomeDTO> homes = plugin.getHomeCache().getHomesByPlayer(p.getUniqueId());
        if(homes==null){
            p.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.no-homes", "you dont have homes")));
            return;
        }
        for(HomeDTO home : homes){
            items.add(createHomeItem(home));
        }
        inv.addItems(items);
        inv.open();

    }

    private ItemStack createHomeItemStack(HomeDTO home){
        Material material = Material.getMaterial(plugin.getConfig().getString("ui.home-item.material", "RED_BED"));
        Component name = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("ui.home-item.name", "<!italic>{NAME}")
                .replace("{X}", ""+home.location().getBlockX())
                .replace("{Y}", ""+home.location().getBlockY())
                .replace("{Z}", ""+home.location().getBlockZ())
                .replace("{NAME}", home.displayName())
        );
        List<Component> lore = new ArrayList<>();
        for(String s : plugin.getConfig().getStringList("ui.home-item.lore")){
            lore.add(MiniMessage.miniMessage().deserialize(s
                    .replace("{X}", ""+home.location().getBlockX())
                    .replace("{Y}", ""+home.location().getBlockY())
                    .replace("{Z}", ""+home.location().getBlockZ())
                    .replace("{NAME}", home.displayName())
            ));
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ClickableInventoryV4.ClickableItem createHomeItem(HomeDTO home){
        ItemStack item = createHomeItemStack(home);

        return new ClickableInventoryV4.ClickableItem(item, action ->{
            Bukkit.getLogger().info("Clicktype: " + action.getClickType().name());
            if(action.getClickType()== ClickType.RIGHT){
                action.getPlayer().getInventory().close();
                openDeleteConfirm(action.getPlayer(), home);
            }
            if(action.isLeftClick()){
                action.getPlayer().getInventory().close();
                queTeleport(action.getPlayer(), home.location());
            }
        });
    }

    public void openCreateMenu(Player p){

    }

    public void attemptCreateHome(Player p, String name){

        int maxHomes = getMaxHomes(p);
        if(maxHomes<0) maxHomes = plugin.getConfig().getInt("default-homes", 5);
        if( plugin.getHomeCache().getHomesByPlayer(p.getUniqueId())!=null&& maxHomes<=plugin.getHomeCache().getHomesByPlayer(p.getUniqueId()).size()){
            p.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.homes-limit-reached", "<red>you cannot create any more homes").replace("{MAX}",""+ getMaxHomes(p))));
            return;
        }
        HomeDTO result = new HomeDTO(UUID.randomUUID(), p.getUniqueId(), p.getLocation(), name, LocalDateTime.now());
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            plugin.getHomeService().queueChanges(result);
            p.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home-created", "<green>Successfully created.").replace("{HOME}", result.displayName())));
        });
    }

    private void openDeleteConfirm(Player p, HomeDTO h){
        Bukkit.getLogger().info("dispalyname: "+ h.displayName());
        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("ui.delete-confirm.confirm-item.name", "confirm").replace("{NAME}", h.displayName())));
        List<Component> confirmLore = new ArrayList<>();
        for(String s : plugin.getConfig().getStringList("ui.delete-confirm.confirm-item.lore")){
            confirmLore.add(MiniMessage.miniMessage().deserialize(s.replace("{NAME}", h.displayName())));
        }
        confirmMeta.lore(confirmLore);
        confirm.setItemMeta(confirmMeta);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("ui.delete-confirm.cancel-item.name", "cancel").replace("{NAME}", h.displayName())));
        List<Component> cancelLore = new ArrayList<>();
        for(String s : plugin.getConfig().getStringList("ui.delete-confirm.cancel-item.lore")){
            cancelLore.add(MiniMessage.miniMessage().deserialize(s.replace("{NAME}", h.displayName())));
        }
        cancelMeta.lore(cancelLore);
        cancel.setItemMeta(cancelMeta);

        ClickableInventoryV4 inv = plugin.getInventoryManager().create(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("ui.delete-confirm.title", "<red>Error").replace("{NAME}", h.displayName())), p);
        inv.setLayoutStyle(ClickableInventoryV4.LayoutStyle.GAMEMODES_3)
                .setUsableRows(3)
                .setShowNavigation(false)
                .addItem(confirm, action ->{
                    action.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.delete-confirm", "remove success").replace("{HOME}", h.displayName())));
                    inv.close();
                    Bukkit.getAsyncScheduler().runNow(plugin, task ->{
                        plugin.getHomeRepository().removeHome(h.homeId());
                    });
                })
                .addItem(createHomeItemStack(h), action ->{
                    if(action.getClickType()== ClickType.RIGHT){
                        action.getPlayer().getInventory().close();
                        openDeleteConfirm(action.getPlayer(), h);
                    }
                    if(action.isLeftClick()){
                        action.getPlayer().getInventory().close();
                        queTeleport(action.getPlayer(), h.location());
                    }
                })
                .addItem(cancel, action ->{
                    inv.close();
                });
        inv.open();

    }

    public void queTeleport(Player p, Location target){
        plugin.getTpManager().teleportToLocation(p, target, plugin.getConfig().getInt("teleport-warmup", 3));
    }

    public static int getMaxHomes(Player player) {
        int max = 0;
        for (int i = 0; i <= 128; i++) {
            if (player.hasPermission("homes.limit." + i)) {
                max = i;
            }
        }
        return max;
    }
}
