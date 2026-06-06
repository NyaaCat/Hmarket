package cat.nyaa.hmarket.ui;

import cat.nyaa.hmarket.HMI18n;
import cat.nyaa.hmarket.Hmarket;
import cat.nyaa.hmarket.api.IMarketAPI;
import cat.nyaa.hmarket.api.data.MarketBuyResult;
import cat.nyaa.hmarket.ui.data.ShopItemDataUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static cat.nyaa.hmarket.api.data.MarketBuyResult.MarketBuyStatus.NOT_ENOUGH_MONEY;
import static cat.nyaa.hmarket.api.data.MarketBuyResult.MarketBuyStatus.WITHDRAW_SUCCESS;

public class HmarketShopView {
    private static final ItemStack iconLoading;
    static final ItemStack iconRefresh;
    static final ItemStack iconNextPage;
    static final ItemStack iconPrevPage;
    private static final ItemStack iconNotAvail; // which is bought by others

    private static final ItemStack iconEmptyStore;
    private static final ItemStack iconPending;
    private static final ItemStack iconPurchased; // which is bought by users self
    private static final ItemStack iconWithdrawn;
    private static final ItemStack iconError;

    static {
        iconLoading = new ItemStack(Material.CALIBRATED_SCULK_SENSOR, 1);
        var meta = iconLoading.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.loading"));
        iconLoading.setItemMeta(meta);
        iconLoading.lore(List.of(HMI18n.format("info.ui.element.loading_description")));

        iconRefresh = new ItemStack(Material.BRUSH, 1);
        meta = iconRefresh.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.refresh"));
        iconRefresh.setItemMeta(meta);
        iconRefresh.lore(List.of(HMI18n.format("info.ui.element.refresh_description")));

        iconNextPage = new ItemStack(Material.MAP, 1);
        meta = iconNextPage.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.next_page"));
        iconNextPage.setItemMeta(meta);

        iconPrevPage = new ItemStack(Material.FILLED_MAP, 1);
        meta = iconPrevPage.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.previous_page"));
        iconPrevPage.setItemMeta(meta);

        iconNotAvail = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        meta = iconNotAvail.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.not_available"));
        iconNotAvail.setItemMeta(meta);
        iconNotAvail.lore(List.of(HMI18n.format("info.ui.element.not_available_description")));

        iconEmptyStore = new ItemStack(Material.GLASS_BOTTLE, 1);
        meta = iconEmptyStore.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.empty_store"));
        iconEmptyStore.setItemMeta(meta);
        iconEmptyStore.lore(List.of(HMI18n.format("info.ui.element.empty_store_description")));

        iconPending = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE, 1);
        meta = iconPending.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.pending"));
        iconPending.setItemMeta(meta);
        iconPending.lore(List.of(HMI18n.format("info.ui.element.pending_description")));

        iconPurchased = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);
        meta = iconPurchased.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.purchased"));
        iconPurchased.setItemMeta(meta);
        iconPurchased.lore(List.of(HMI18n.format("info.ui.element.purchased_description")));

        iconWithdrawn = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1);
        meta = iconWithdrawn.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.withdrawn"));
        iconWithdrawn.setItemMeta(meta);
        iconWithdrawn.lore(List.of(HMI18n.format("info.ui.element.withdrawn_description")));

        iconError = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        meta = iconError.getItemMeta();
        meta.displayName(HMI18n.format("info.ui.element.action_needed"));
        iconError.setItemMeta(meta);
    }

       private final IMarketAPI api;
    private final Inventory ui;
    private final Player viewOwner;
    private final List<ItemStack> items = new ArrayList<>();
    private final UUID viewShopID;
    private int currentPage = 1;
    private ItemStack pendingItemStack;
    private int pendingSlot;


    public HmarketShopView(Player viewOwner, UUID shopUniqueID, Component title) {
        this.viewOwner = viewOwner;
        api = Hmarket.getAPI().getMarketAPI();
        ui = Bukkit.createInventory(null, 6 * 9, title);
        viewShopID = shopUniqueID;
        reloadShopItems(viewShopID);
    }

    private void reloadShopItems(UUID shopUniqueID) {
        resetUI();
        Bukkit.getScheduler().runTaskAsynchronously(Hmarket.getInstance(), () -> {
            try {
                items.clear();
                items.addAll(api.getShopItems(shopUniqueID).get().stream().map(t -> ShopItemDataUtils.getWindowedItem(this.viewOwner, t)).toList());
                renderPage(currentPage);
            } catch (InterruptedException | ExecutionException e) {
                closeUiIfErrorOccurred(e);
            }
        });
    }

    private void resetUI() {
        ui.clear();
        ui.setItem(22, iconLoading);
    }

    private boolean hasNextPage() {
        return currentPage < getMaximumPage();
    }

    private int getMaximumPage() {
        return (items.size() - 1) / 45 + 1;
    }

    private boolean hasPrevPage() {
        return currentPage > 1;
    }

    public HmarketConfirmPurchaseView createConfirmView(Player player, InventoryAction action, ItemStack itemStack, int slot) {
        //click to buy 1
        //right click to buy a half
        //shift click to buy all
        final int amount = switch (action) {
            case PICKUP_ALL -> 1;
            case PICKUP_HALF -> {
                var half = itemStack.getAmount() / 2;
                yield half * 2 == itemStack.getAmount() ? half : half + 1;
            }
            case MOVE_TO_OTHER_INVENTORY -> itemStack.getAmount();
            default -> -1;
        };
        if (amount == -1)
            return null;
        int marketItemId = ShopItemDataUtils.getMarketItemIDFromItemStack(itemStack);
        pendingItemStack = itemStack.clone();
        pendingSlot = slot;
        ui.setItem(slot, iconPending);
        return new HmarketConfirmPurchaseView(player, this, viewShopID, marketItemId, amount, slot, itemStack);
    }

    public void returnFromConfirmation(int shopSlot, ItemStack originalItem) {
        setItemInCurrentPage(shopSlot, originalItem);
        renderPage(currentPage);
    }

    public void onPurchaseResult(MarketBuyResult result, int shopSlot, int amount, ItemStack itemStack) {
        switch (result.status()) {
            case WITHDRAW_SUCCESS, SUCCESS -> {
                if (amount == itemStack.getAmount()) {
                    setItemInCurrentPage(shopSlot, result.status() == WITHDRAW_SUCCESS ? iconWithdrawn : iconPurchased);
                } else {
                    itemStack.setAmount(itemStack.getAmount() - amount);
                    setItemInCurrentPage(shopSlot, itemStack);
                }
            }
            case OUT_OF_STOCK, ITEM_NOT_FOUND -> {
                setItemInCurrentPage(shopSlot, iconNotAvail);
            }
            case NOT_ENOUGH_MONEY, TASK_FAILED, TRANSACTION_ERROR, WRONG_MARKET, CANNOT_BUY_ITEM -> {
                var icon = iconError.clone();
                if (result.status() == NOT_ENOUGH_MONEY) {
                    icon.lore(List.of(HMI18n.format("info.ui.market.not_enough_money")));
                } else {
                    icon.lore(List.of(HMI18n.format("info.ui.element.action_needed_description", result.status())));
                }
                setItemInCurrentPage(shopSlot, icon);
            }
        }
        renderPage(currentPage);
    }

    private void setItemInCurrentPage(int slot, ItemStack itemStack) {
        items.set(slot + (currentPage - 1) * 45, itemStack);
    }

    private void closeUiIfErrorOccurred(Exception exception) {
        exception.printStackTrace();
        if (this.viewOwner.getOpenInventory().getTopInventory() == ui) {
            this.viewOwner.closeInventory();
        }
        this.viewOwner.sendMessage(HMI18n.format("info.ui.market.internal_error_occurred", exception.toString()));
    }

    private void renderPage(int page) {
        //put items at 1-5 line
        //put buttons at line 6
        ui.clear();

        ui.setItem(50, iconRefresh);
        if (hasPrevPage())
            ui.setItem(52, iconPrevPage);
        if (hasNextPage())
            ui.setItem(53, iconNextPage);

        if (items.isEmpty()) {
            ui.addItem(iconEmptyStore);
        } else {
            for (int i = 0; i < 45; i++) {
                int index = i + (page - 1) * 45;
                if (index >= items.size()) {
                    break;
                }
                ui.setItem(i, items.get(index));
            }
        }
    }

    public Inventory getUi() {
        return ui;
    }

    public void onPageChange(InventoryAction action, ItemStack itemStack, int slot) {
        if (itemStack.equals(iconNextPage)) {
            if (hasNextPage())
                currentPage++;
            renderPage(currentPage);
        } else if (itemStack.equals(iconPrevPage)) {
            if (hasPrevPage())
                currentPage--;
            renderPage(currentPage);
        }
    }

    public void onRefresh(InventoryAction action, ItemStack itemStack, int slot) {
        reloadShopItems(viewShopID);
    }

}
