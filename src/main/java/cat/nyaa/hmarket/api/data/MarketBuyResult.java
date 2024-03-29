package cat.nyaa.hmarket.api.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record MarketBuyResult(cat.nyaa.hmarket.api.data.MarketBuyResult.MarketBuyStatus status) {
    public MarketBuyResult(@NotNull MarketBuyStatus status) {
        this.status = status;
    }


    public static @NotNull MarketBuyResult fail(@NotNull MarketBuyStatus status) {
        if (status == MarketBuyStatus.SUCCESS) throw new IllegalArgumentException("status can't be SUCCESS");
        return new MarketBuyResult(status);
    }

    @Contract(" -> new")
    public static @NotNull MarketBuyResult success() {
        return success(false);
    }

    public static @NotNull MarketBuyResult success(boolean withdraw) {
        if (withdraw) {
            return new MarketBuyResult(MarketBuyStatus.WITHDRAW_SUCCESS);
        }
        return new MarketBuyResult(MarketBuyStatus.SUCCESS);
    }

    public boolean isSuccess() {
        return status == MarketBuyStatus.SUCCESS || status == MarketBuyStatus.WITHDRAW_SUCCESS;
    }

    public enum MarketBuyStatus {
        SUCCESS,
        OUT_OF_STOCK,
        NOT_ENOUGH_MONEY,
        TASK_FAILED,
        ITEM_NOT_FOUND,
        TRANSACTION_ERROR,
        WRONG_MARKET,
        PLAYER_OWNS_ITEM, WITHDRAW_SUCCESS, CANNOT_BUY_ITEM
    }
}
