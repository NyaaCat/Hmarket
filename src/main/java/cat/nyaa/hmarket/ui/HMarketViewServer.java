package cat.nyaa.hmarket.ui;

import cat.nyaa.hmarket.HMI18n;
import cat.nyaa.hmarket.ui.data.ShopItemDataUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class HMarketViewServer implements Listener {
    private final JavaPlugin pluginInstance;

    private final Map<UUID, HmarketShopView> viewMap = new HashMap<>();
    private final Map<UUID, HmarketConfirmPurchaseView> confirmViewMap = new HashMap<>();
    private final Set<UUID> interactedPlayers = new HashSet<>();

    private final BukkitTask resetTask;

    public HMarketViewServer(JavaPlugin pluginInstance) {
        this.pluginInstance = pluginInstance;
        //clear interactedPlayers set every tick to prevent clicks caused by accident
        resetTask = Bukkit.getScheduler().runTaskTimer(pluginInstance, interactedPlayers::clear, 0L, 1L);
    }

    public void openViewForPlayer(Player player) {
        player.openInventory(viewMap.get(player.getUniqueId()).getUi());
    }

    public void createViewForPlayer(Player player, UUID marketId, Component title) {
        confirmViewMap.remove(player.getUniqueId());
        viewMap.put(player.getUniqueId(), new HmarketShopView(player, marketId, title));
    }

    public void openConfirmViewForPlayer(Player player, HmarketConfirmPurchaseView confirmView) {
        confirmViewMap.put(player.getUniqueId(), confirmView);
        player.openInventory(confirmView.getUi());
    }

    public void destrutor() {
        viewMap.values().forEach(t -> t.getUi().close());
        viewMap.clear();
        resetTask.cancel();
    }

    public Component getUserShopTitle(UUID playerUniqueID) {
        var playerName = Bukkit.getOfflinePlayer(playerUniqueID);
        return HMI18n.format("info.ui.title.shop.user", playerName);
    }

    public Component getSystemShopTitle(UUID playerUniqueID) {
        var playerName = Bukkit.getOfflinePlayer(playerUniqueID);
        return HMI18n.format("info.ui.title.shop.system");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;
        var playerId = event.getWhoClicked().getUniqueId();

        // Handle confirmation view clicks first
        if (confirmViewMap.containsKey(playerId)) {
            var confirmView = confirmViewMap.get(playerId);
            if (event.getInventory() == confirmView.getUi()) {
                event.setCancelled(true);
                var item = event.getCurrentItem();
                if (item == null || item.getType().isAir()) return;
                if (interactedPlayers.contains(playerId))
                    return;
                confirmView.onClick((Player) event.getWhoClicked(), item, event.getSlot());
                interactedPlayers.add(playerId);
                return;
            }
        }

        // Handle shop view clicks
        if (!viewMap.containsKey(playerId))
            return;
        if (event.getInventory() != viewMap.get(playerId).getUi())
            return;
        if (event.getClickedInventory() == viewMap.get(playerId).getUi()) {
            event.setCancelled(true);
            var item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            if (interactedPlayers.contains(playerId))
                return; //can interact with ui only once per tick
            var shopView = viewMap.get(playerId);
            if (item.equals(HmarketShopView.iconNextPage)) {
                shopView.onPageChange(event.getAction(), item, event.getSlot());
            } else if (item.equals(HmarketShopView.iconPrevPage)) {
                shopView.onPageChange(event.getAction(), item, event.getSlot());
            } else if (item.equals(HmarketShopView.iconRefresh)) {
                shopView.onRefresh(event.getAction(), item, event.getSlot());
            } else if (ShopItemDataUtils.checkIfIsWindowedItem(item)) {
                var confirmView = shopView.createConfirmView((Player) event.getWhoClicked(), event.getAction(), item, event.getSlot());
                if (confirmView != null) {
                    openConfirmViewForPlayer((Player) event.getWhoClicked(), confirmView);
                }
            }
            interactedPlayers.add(playerId);
        }
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (viewMap.containsKey(event.getWhoClicked().getUniqueId())
                && event.getInventory() == viewMap.get(event.getWhoClicked().getUniqueId()).getUi())
            if (event.getNewItems().keySet().stream().anyMatch(t -> t < 54))
                event.setCancelled(true);
    }

//    @EventHandler
//    public void onInventoryMove(InventoryMoveItemEvent event) {
//        if (event.getSource().getHolder() instanceof Player player) {
//            if (event.getDestination() == viewMap.get(player.getUniqueId()).getUi())
//                event.setCancelled(true);
//        }
//
//    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        if (confirmViewMap.containsKey(playerId)) {
            if (event.getInventory() == confirmViewMap.get(playerId).getUi()) {
                confirmViewMap.remove(playerId);
                // When closed via X button, return to shop view
                if (viewMap.containsKey(playerId)) {
                    event.getPlayer().openInventory(viewMap.get(playerId).getUi());
                }
            }
            return;
        }
        if (viewMap.containsKey(playerId))
            if (event.getInventory() == viewMap.get(playerId).getUi()) {
                viewMap.remove(playerId);
            }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        confirmViewMap.remove(event.getPlayer().getUniqueId());
        viewMap.remove(event.getPlayer().getUniqueId());
    }

}
