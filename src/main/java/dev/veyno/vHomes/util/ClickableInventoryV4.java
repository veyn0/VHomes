package dev.veyno.vHomes.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;


public class ClickableInventoryV4 implements Listener {


    private final InventoryManager manager;


    private static final int SLOTS_PER_ROW = 9;
    private static final int NAVIGATION_ROW_SIZE = 9;

    private static final int PREV_PAGE_SLOT_OFFSET = 3;
    private static final int CURRENT_PAGE_SLOT_OFFSET = 4;
    private static final int NEXT_PAGE_SLOT_OFFSET = 5;

    private final Component title;
    private final Player player;
    private final List<ClickableItem> items;
    private final Map<Integer, Consumer<ClickContext>> slotActions;
    private final Map<Integer, StaticNavigationItem> staticNavigationItems;
    private int currentPage;
    private Inventory inventory;
    private LayoutStyle layoutStyle;
    private ItemStack fillerItem;
    private int usableRows; // Anzahl der nutzbaren Reihen (1-5)
    private boolean hideNavigationOnSinglePage; // Navigation ausblenden wenn nur eine Seite
    private boolean showNavigation; // Ob Navigation angezeigt werden soll


    public ClickableInventoryV4(InventoryManager manager, Component title, Player player) {
        this.manager = manager;
        this.title = title;
        this.player = player;
        this.items = new ArrayList<>();
        this.slotActions = new HashMap<>();
        this.staticNavigationItems = new HashMap<>();
        this.currentPage = 0;
        this.layoutStyle = LayoutStyle.STANDARD;
        this.fillerItem = createNavigationItem(Material.GRAY_STAINED_GLASS_PANE, "§r", "");
        this.usableRows = 5; // Standard: 5 Reihen
        this.hideNavigationOnSinglePage = false;
        this.showNavigation = true;
    }

    public ClickableInventoryV4 setUsableRows(int rows) {
        if (rows < 1 || rows > 5) {
            throw new IllegalArgumentException("Anzahl der Reihen muss zwischen 1 und 5 liegen!");
        }
        this.usableRows = rows;
        return this;
    }

    public ClickableInventoryV4 setHideNavigationOnSinglePage(boolean hide) {
        this.hideNavigationOnSinglePage = hide;
        return this;
    }

    public ClickableInventoryV4 setShowNavigation(boolean show) {
        this.showNavigation = show;
        return this;
    }

    public int getUsableRows() {
        return usableRows;
    }

    public ClickableInventoryV4 addItem(ItemStack itemStack, Consumer<ClickContext> action) {
        items.add(new ClickableItem(itemStack, action));
        return this;
    }

    public ClickableInventoryV4 addItem(ItemStack itemStack, Runnable action) {
        items.add(new ClickableItem(itemStack, ctx -> action.run()));
        return this;
    }

    public ClickableInventoryV4 addItems(List<ClickableItem> clickableItems) {
        items.addAll(clickableItems);
        return this;
    }

    public ClickableInventoryV4 addStaticNavigationItem(int slot, ItemStack itemStack, Consumer<ClickContext> action) {
        if (slot < 0 || slot >= NAVIGATION_ROW_SIZE) {
            throw new IllegalArgumentException("Slot muss zwischen 0 und 8 sein!");
        }
        if (slot == PREV_PAGE_SLOT_OFFSET || slot == CURRENT_PAGE_SLOT_OFFSET || slot == NEXT_PAGE_SLOT_OFFSET) {
            throw new IllegalArgumentException("Slot " + slot + " ist für die Navigation reserviert!");
        }
        staticNavigationItems.put(slot, new StaticNavigationItem(itemStack, action));
        return this;
    }

    public ClickableInventoryV4 addStaticNavigationItem(int slot, Material material, String name, Consumer<ClickContext> action, String... lore) {
        ItemStack item = createNavigationItem(material, name, lore);
        return addStaticNavigationItem(slot, item, action);
    }

    public ClickableInventoryV4 addStaticNavigationItem(int slot, ItemStack itemStack, Runnable action) {
        return addStaticNavigationItem(slot, itemStack, ctx -> action.run());
    }

    public ClickableInventoryV4 addStaticNavigationItem(int slot, Material material, String name, Runnable action, String... lore) {
        return addStaticNavigationItem(slot, material, name, ctx -> action.run(), lore);
    }

    public ClickableInventoryV4 removeStaticNavigationItem(int slot) {
        staticNavigationItems.remove(slot);
        return this;
    }

    public ClickableInventoryV4 clearStaticNavigationItems() {
        staticNavigationItems.clear();
        return this;
    }

    public ClickableInventoryV4 setLayoutStyle(LayoutStyle layoutStyle) {
        this.layoutStyle = layoutStyle;
        return this;
    }

    public ClickableInventoryV4 setFillerItem(ItemStack fillerItem) {
        this.fillerItem = fillerItem;
        return this;
    }

    public ClickableInventoryV4 setFillerItem(Material material, String name) {
        this.fillerItem = createItem(material, name);
        return this;
    }

    public void open() {
        if (player == null) return;

        manager.registerInventory(player.getUniqueId(), this);
        updateInventory();
        player.openInventory(inventory);
    }

    public void close() {
        if (player != null) {
            player.closeInventory();
            manager.unregisterInventory(player.getUniqueId());
        }
    }

    public void nextPage() {
        int maxPages = getMaxPages();
        if (currentPage < maxPages - 1) {
            currentPage++;
            updateInventory();
            if (player != null) {
                Bukkit.getRegionScheduler().run(manager.getPlugin(), player.getLocation(), task -> {
                    player.openInventory(inventory);
                });
            }
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            updateInventory();
            if (player != null) {
                Bukkit.getRegionScheduler().run(manager.getPlugin(), player.getLocation(), task -> player.openInventory(inventory));
            }
        }
    }

    public void setPage(int page) {
        int maxPages = getMaxPages();
        if (page >= 0 && page < maxPages) {
            currentPage = page;
            updateInventory();
            if (player != null) {
                Bukkit.getRegionScheduler().run(manager.getPlugin(), player.getLocation(), task -> player.openInventory(inventory));
            }
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getMaxPages() {
        int itemsPerPage = layoutStyle.getItemsPerPage(usableRows);
        return Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
    }

    private int getInventorySize() {
        boolean shouldShowNavigation = showNavigation && (!hideNavigationOnSinglePage || getMaxPages() > 1);
        int rows = shouldShowNavigation ? usableRows + 1 : usableRows;
        return rows * SLOTS_PER_ROW;
    }

    private void updateInventory() {
        boolean shouldShowNavigation = showNavigation && (!hideNavigationOnSinglePage || getMaxPages() > 1);

        Component inventoryTitle = shouldShowNavigation
                ? title.append(Component.text(" (Seite " + (currentPage + 1) + "/" + getMaxPages() + ")"))
                : title;

        int inventorySize = getInventorySize();
        inventory = Bukkit.createInventory(null, inventorySize, inventoryTitle);
        slotActions.clear();

        int itemsPerPage = layoutStyle.getItemsPerPage(usableRows);
        int totalItemSlots = usableRows * SLOTS_PER_ROW;

        if (layoutStyle.usesFiller()) {
            for (int i = 0; i < totalItemSlots; i++) {
                inventory.setItem(i, fillerItem);
            }
        }

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        List<Integer> availableSlots = layoutStyle.getAvailableSlots(usableRows);
        int slotIndex = 0;

        for (int i = startIndex; i < endIndex && slotIndex < availableSlots.size(); i++, slotIndex++) {
            int slot = availableSlots.get(slotIndex);
            ClickableItem clickableItem = items.get(i);
            inventory.setItem(slot, clickableItem.getItemStack());
            slotActions.put(slot, clickableItem.getAction());
        }

        if (shouldShowNavigation) {
            setupNavigation();
            setupStaticNavigationItems();
        }
    }

    private void setupNavigation() {
        int navigationRowStart = usableRows * SLOTS_PER_ROW;

        //TODO: add config variables for different languages etc.

        if (currentPage > 0) {
            ItemStack prevArrow = createNavigationItem(Material.ARROW, "§aPrevious Page",
                    "§7Page " + currentPage + "/" + getMaxPages());
            inventory.setItem(navigationRowStart + PREV_PAGE_SLOT_OFFSET, prevArrow);
            slotActions.put(navigationRowStart + PREV_PAGE_SLOT_OFFSET, ctx -> previousPage());
        }

        ItemStack pageInfo = createNavigationItem(Material.PAPER, "§ePage " + (currentPage + 1),
                "§7of " + getMaxPages() + " Pages",
                "§7total " + items.size() + " Options");
        inventory.setItem(navigationRowStart + CURRENT_PAGE_SLOT_OFFSET, pageInfo);

        if (currentPage < getMaxPages() - 1) {
            ItemStack nextArrow = createNavigationItem(Material.ARROW, "§aNext page",
                    "§7Page " + (currentPage + 2) + "/" + getMaxPages());
            inventory.setItem(navigationRowStart + NEXT_PAGE_SLOT_OFFSET, nextArrow);
            slotActions.put(navigationRowStart + NEXT_PAGE_SLOT_OFFSET, ctx -> nextPage());
        }

        ItemStack separator = createNavigationItem(Material.GRAY_STAINED_GLASS_PANE, "§r", "");
        for (int i = navigationRowStart; i < navigationRowStart + NAVIGATION_ROW_SIZE; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, separator);
            }
        }
    }

    private void setupStaticNavigationItems() {
        int navigationRowStart = usableRows * SLOTS_PER_ROW;
        for (Map.Entry<Integer, StaticNavigationItem> entry : staticNavigationItems.entrySet()) {
            int relativeSlot = entry.getKey();
            int absoluteSlot = navigationRowStart + relativeSlot;
            StaticNavigationItem item = entry.getValue();
            inventory.setItem(absoluteSlot, item.getItemStack());
            slotActions.put(absoluteSlot, item.getAction());
        }
    }

    private ItemStack createNavigationItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected Inventory getInventory() {
        return inventory;
    }

    protected void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        int inventorySize = getInventorySize();

        if (slot < 0 || slot >= inventorySize) {
            return;
        }

        Consumer<ClickContext> action = slotActions.get(slot);

        if (action != null) {
            ClickContext context = new ClickContext(
                    (Player) event.getWhoClicked(),
                    event.getClick(),
                    event.getSlot(),
                    event.getCurrentItem(),
                    event.isShiftClick(),
                    event.isLeftClick(),
                    event.isRightClick()
            );

            Bukkit.getRegionScheduler().run(manager.getPlugin(), event.getWhoClicked().getLocation(), task -> action.accept(context));
        }
    }

    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class ClickContext {
        private final Player player;
        private final ClickType clickType;
        private final int slot;
        private final ItemStack clickedItem;
        private final boolean shiftClick;
        private final boolean leftClick;
        private final boolean rightClick;

        public ClickContext(Player player, ClickType clickType, int slot, ItemStack clickedItem,
                            boolean shiftClick, boolean leftClick, boolean rightClick) {
            this.player = player;
            this.clickType = clickType;
            this.slot = slot;
            this.clickedItem = clickedItem;
            this.shiftClick = shiftClick;
            this.leftClick = leftClick;
            this.rightClick = rightClick;
        }

        public Player getPlayer() { return player; }
        public ClickType getClickType() { return clickType; }
        public int getSlot() { return slot; }
        public ItemStack getClickedItem() { return clickedItem; }
        public boolean isShiftClick() { return shiftClick; }
        public boolean isLeftClick() { return leftClick; }
        public boolean isRightClick() { return rightClick; }
        public boolean isMiddleClick() { return clickType == ClickType.MIDDLE; }
        public boolean isDropClick() { return clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP; }
        public boolean isNumberKey() { return clickType.isKeyboardClick(); }
        public boolean isDoubleClick() { return clickType == ClickType.DOUBLE_CLICK; }
    }

    public static enum LayoutStyle {
        STANDARD {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < rows * 9; i++) {
                    slots.add(i);
                }
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                return rows * 9;
            }

            @Override
            public boolean usesFiller() {
                return false;
            }
        },

        BORDERED {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                int usableRows = Math.max(1, rows - 2);
                for (int row = 1; row <= usableRows; row++) {
                    for (int col = 1; col < 8; col++) {
                        slots.add(row * 9 + col);
                    }
                }
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                int usableRows = Math.max(1, rows - 2);
                return usableRows * 7;
            }

            @Override
            public boolean usesFiller() {
                return true;
            }
        },

        COLUMNS {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                for (int row = 0; row < rows; row++) {
                    for (int col = 1; col < 9; col += 2) {
                        slots.add(row * 9 + col);
                    }
                }
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                return rows * 4;
            }

            @Override
            public boolean usesFiller() {
                return true;
            }
        },

        ROWS {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                for (int row = 1; row < rows; row += 2) {
                    for (int col = 0; col < 9; col++) {
                        slots.add(row * 9 + col);
                    }
                }
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                return ((rows - 1) / 2 + 1) * 9;
            }

            @Override
            public boolean usesFiller() {
                return true;
            }
        },

        GRID_3X3 {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                for (int row = 0; row < Math.min(3, rows); row++) {
                    for (int col = 0; col < 9; col += 3) {
                        slots.add(row * 9 + col);
                    }
                }
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                return Math.min(3, rows) * 3;
            }

            @Override
            public boolean usesFiller() {
                return true;
            }
        },


        GAMEMODES_3 {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                int padding = (int)(rows-1)/2;
                int startSlot = (padding*9)-1;
                slots.addAll(Arrays.asList(startSlot+3, startSlot+5, startSlot+7));
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                return 3;
            }

            @Override
            public boolean usesFiller() {
                return true;
            }
        },

        GAMEMODES_4 {
            @Override
            public List<Integer> getAvailableSlots(int rows) {
                List<Integer> slots = new ArrayList<>();
                int padding = (int)(rows-1)/2;
                int startSlot = (padding*9)-1;
                slots.addAll(Arrays.asList(startSlot+2, startSlot+4, startSlot+6, startSlot+8));
                return slots;
            }

            @Override
            public int getItemsPerPage(int rows) {
                return 4;
            }

            @Override
            public boolean usesFiller() {
                return true;
            }
        };

        public abstract List<Integer> getAvailableSlots(int rows);

        public abstract int getItemsPerPage(int rows);

        public abstract boolean usesFiller();
    }

    public static class ClickableItem {
        private final ItemStack itemStack;
        private final Consumer<ClickContext> action;

        public ClickableItem(ItemStack itemStack, Consumer<ClickContext> action) {
            this.itemStack = itemStack.clone();
            this.action = action;
        }

        public ItemStack getItemStack() {
            return itemStack.clone();
        }

        public Consumer<ClickContext> getAction() {
            return action;
        }
    }

    private static class StaticNavigationItem {
        private final ItemStack itemStack;
        private final Consumer<ClickContext> action;

        public StaticNavigationItem(ItemStack itemStack, Consumer<ClickContext> action) {
            this.itemStack = itemStack.clone();
            this.action = action;
        }

        public ItemStack getItemStack() {
            return itemStack.clone();
        }

        public Consumer<ClickContext> getAction() {
            return action;
        }
    }

    public static class InventoryManager implements Listener {
        private final JavaPlugin plugin;
        private final Map<UUID, ClickableInventoryV4> activeInventories;

        public InventoryManager(JavaPlugin plugin) {
            this.plugin = plugin;
            this.activeInventories = new HashMap<>();
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        public JavaPlugin getPlugin() {
            return plugin;
        }

        protected void registerInventory(UUID playerId, ClickableInventoryV4 inventory) {
            activeInventories.put(playerId, inventory);
        }

        protected void unregisterInventory(UUID playerId) {
            activeInventories.remove(playerId);
        }

        public ClickableInventoryV4 create(Component title, Player player) {
            return new ClickableInventoryV4(this, title, player);
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player clicker = (Player) event.getWhoClicked();
            ClickableInventoryV4 clickableInventory = activeInventories.get(clicker.getUniqueId());

            if (clickableInventory == null || !event.getInventory().equals(clickableInventory.getInventory())) {
                return;
            }

            clickableInventory.handleClick(event);
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player)) return;

            Player player = (Player) event.getPlayer();
            ClickableInventoryV4 clickableInventory = activeInventories.get(player.getUniqueId());

            if (clickableInventory != null && event.getInventory().equals(clickableInventory.getInventory())) {
                activeInventories.remove(player.getUniqueId());
            }
        }
    }
}