package cat.nyaa.hmarket.listener;

import cat.nyaa.hmarket.Hmarket;
import cat.nyaa.hmarket.api.IMarketShopLocation;
import cat.nyaa.hmarket.utils.HMLogUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public class HMBlockEnvironmentListener implements Listener {
    private static Optional<IMarketShopLocation> getShopLocation() {
        return Optional.ofNullable(Hmarket.getAPI() != null ? Hmarket.getAPI().getShopLocationApi() : null);
    }

    private static boolean isBlockProtected(Block block, @Nullable Player player) {
        return getShopLocation().map(shopLocation -> shopLocation.isBlockProtected(block, player)).orElseGet(() -> {
            HMLogUtils.logWarning("HMarket is not loaded, can not break block");
            return true;
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isBlockProtected(block, null));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isBlockProtected(block, null));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStructureGrow(@NotNull StructureGrowEvent event) {
        for (BlockState blockstate : event.getBlocks()) {
            if (isBlockProtected(blockstate.getBlock(), null)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(@NotNull BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isBlockProtected(block, null)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(@NotNull BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isBlockProtected(block, null)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {
        if (isBlockProtected(event.getBlock(), null)) {
            event.setCancelled(true);
        }
    }

    //    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
//    public void onBlockPhysics(@NotNull BlockPhysicsEvent event) {
//        if (isBlockProtected(event.getBlock(), null)) {
//            event.setCancelled(true);
//        }
//    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isBlockProtected(block, event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
