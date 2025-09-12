package cat.nyaa.hmarket.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HMInventoryUtils {
    public static void giveOrDropItem(Player player, ItemStack itemStack) {
        var result = player.getInventory().addItem(itemStack);
        result.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), itemStack));
    }
}
