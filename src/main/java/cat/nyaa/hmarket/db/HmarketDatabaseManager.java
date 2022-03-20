package cat.nyaa.hmarket.db;

import cat.nyaa.hmarket.Hmarket;
import cat.nyaa.hmarket.data.ShopItemData;
import cat.nyaa.hmarket.utils.TimeUtils;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class HmarketDatabaseManager {
    public static final String TABLE_SHOP_ITEM = "shop_item";
    private static final LinkedBlockingQueue<Runnable> databaseExecutorQueue = new LinkedBlockingQueue<>();
    public static final ExecutorService databaseExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, databaseExecutorQueue);
    private final Connection connection;
    private final AtomicInteger counter = new AtomicInteger();
    private Hmarket plugin;

    public HmarketDatabaseManager(Hmarket plugin) {

        var optConn = cat.nyaa.aolib.utils.DatabaseUtils.newSqliteJdbcConnection(plugin);
        if (optConn.isEmpty()) {
            throw new RuntimeException("init database error:Failed to connect to database");
        }
        this.connection = optConn.get();
        try {
            this.connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initDatabase(plugin);
    }

    @Contract("_ -> new")
    public static <U> @NotNull CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        if (databaseExecutor.isShutdown()) throw new RuntimeException("playerDataExecutor is shutdown");
        return CompletableFuture.supplyAsync(supplier, databaseExecutor);
    }

    private void initDatabase(Hmarket plugin) {
        this.plugin = plugin;
        cat.nyaa.aolib.utils.DatabaseUtils.executeUpdateAsync(
                connection,
                plugin,
                "init.sql",
                databaseExecutor
        );
    }

    public CompletableFuture<ShopItemData> getShopItemData(int itemId) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeQueryAsync(connection, plugin, "getShopItemById.sql", databaseExecutor, (rs) -> {
            try {
                if (rs.next()) {
                    return ShopItemData.fromResultSet(rs);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }, itemId);
    }

    public CompletableFuture<Boolean> removeItemsFromShop(int itemId, int amount) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeUpdateAsync(connection, plugin, "UpdateShopItemAmount.sql", databaseExecutor, amount, itemId, amount)
                .thenApply((i) -> (i) > 0);
    }

    public CompletableFuture<Optional<Integer>> addItemsToShop(ItemStack items, int amount, UUID ownerId, UUID marketId, double price) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeUpdateAsyncAndGetAutoGeneratedKeys(connection, plugin, "insertShopItem.sql", databaseExecutor, (count, rs) -> {
            if (count <= 0) return Optional.empty();
            try {
                return Optional.of(rs.getInt(1));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, ItemStackUtils.itemToBase64(items), amount, ownerId.toString(), marketId.toString(), price, TimeUtils.getUnixTimeStampNow(), TimeUtils.getUnixTimeStampNow());
    }

    public CompletableFuture<List<ShopItemData>> getAllShopItems(UUID marketId) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeQueryAsync(connection, plugin, "getAllShopItems.sql", databaseExecutor, (rs) -> {
            List<ShopItemData> list = Lists.newArrayList();
            try {
                while (rs.next()) {
                    list.add(ShopItemData.fromResultSet(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }, marketId.toString());
    }

    private UUID getNewItemUUID() {
        return UUID.nameUUIDFromBytes(Bytes.concat(Longs.toByteArray(TimeUtils.getUnixTimeStampNow()), Ints.toByteArray(counter.incrementAndGet())));
    }

    public void close() {
        try {
            this.connection.close();
        } catch (SQLException ignored) {
        }
    }


    public @NotNull CompletableFuture<Integer> removeShopItem(int itemId) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeUpdateAsync(connection, plugin, "removeShopItemById.sql", databaseExecutor, itemId);
    }

    public @NotNull CompletableFuture<List<ShopItemData>> getNeedUpdateItems(long begin) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeQueryAsync(connection, plugin, "getNeedUpdateItems.sql", databaseExecutor, (rs) -> {
            List<ShopItemData> list = Lists.newArrayList();
            try {
                while (rs.next()) {
                    list.add(ShopItemData.fromResultSet(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }, begin);
    }

    public @NotNull CompletableFuture<Integer> setItemUpdateTime(int itemId, long now) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeUpdateAsync(connection, plugin, "setShopItemUpdateTime.sql", databaseExecutor, now, itemId);
    }

    public CompletableFuture<Optional<Integer>> getShopAllItemCount(@NotNull UUID marketId) {

        return cat.nyaa.aolib.utils.DatabaseUtils.executeQueryAsync(connection, plugin, "getAllShopItems.sql", databaseExecutor, (rs) -> {
            try {
                if (rs.next()) {
                    return Optional.of( rs.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, marketId.toString());
    }

    public CompletableFuture<Optional<Integer>> getShopItemCountByOwner(@NotNull UUID marketId, @NotNull UUID ownerId) {
        return cat.nyaa.aolib.utils.DatabaseUtils.executeQueryAsync(connection, plugin, "getShopItemCountByOwner.sql", databaseExecutor, (rs) -> {
            try {
                if (rs.next()) {
                    return Optional.of( rs.getInt(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, marketId.toString(),ownerId.toString());
    }

}
