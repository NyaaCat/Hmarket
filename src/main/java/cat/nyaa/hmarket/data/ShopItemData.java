package cat.nyaa.hmarket.data;


import cat.nyaa.hmarket.api.HMarketAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public record ShopItemData(
        int itemId,
        String itemNbt,
        int amount,
        UUID owner, //owner id
        UUID market, //market uuid
        double price,
        long createdAt, //time
        long updatedAt, //time
        @Nullable String description
) {

    @Contract("_ -> new")
    public static @NotNull ShopItemData fromResultSet(@NotNull ResultSet rs) throws SQLException {
        return new ShopItemData(
                rs.getInt("itemId"),
                rs.getString("itemNbt"),
                rs.getInt("amount"),
                HMarketAPI.parseUUID(rs.getString("owner")),
                HMarketAPI.parseMarketId(rs.getString("market")),
                rs.getDouble("price"),
                rs.getLong("createdAt"),
                rs.getLong("updatedAt"),
                rs.getObject("description") == null ? null : (String) rs.getObject("description")
        );
    }
}
