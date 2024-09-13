package cat.nyaa.hmarket;

import cat.nyaa.hmarket.message.AoMessage;
import cat.nyaa.nyaacore.LanguageRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class HMI18n extends LanguageRepository {
    private static HMI18n instance;
    private final Hmarket plugin;
    private final String language;

    private static final LegacyComponentSerializer legacyComponentSerializer =
            LegacyComponentSerializer.builder().character('§')
                    .useUnusualXRepeatedCharacterHexFormat().build();


    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");


    public HMI18n(Hmarket plugin, String language) {
        instance = this;
        this.plugin = plugin;
        this.language = language;
        load();
    }

    public static Component getComponentOfItem(ItemStack itemStack) {
        Component component;
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
            component = itemStack.displayName();
        } else if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasItemName()) {
            component = itemStack.getItemMeta().itemName();
        } else if (itemStack.getItemMeta() instanceof SkullMeta && ((SkullMeta) itemStack.getItemMeta()).hasOwner()) {
            var key = itemStack.getType().getItemTranslationKey();
            component = Component.translatable(key + ".named");
            component = ((TranslatableComponent)component).args(Component.text(((SkullMeta) itemStack.getItemMeta()).getOwningPlayer().getName()));
        }else{
            component = Component.translatable(Objects.requireNonNull(itemStack.getType().getItemTranslationKey()));
        }
        component = component.hoverEvent(itemStack);
        return component.append(Component.text(" x " + itemStack.getAmount()));
    }

//    private static HoverEvent getHoveredItem(ItemStack itemStack) {
//        byte[] itemBinary;
//        try {
//            itemBinary = ItemStackUtils.itemToBinary(itemStack);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return HoverEvent.showText(Component.text("Error"));
//        }
//        CompoundBinaryTag.builder().
//    }

    @Contract(pure = true)
    public static Component format(String key, Object... _args) {
        if (instance == null)
            return legacyComponentSerializer.deserialize("<Not initialized>");
        var args = Arrays.stream(_args).map(
                o -> {
                    if (o instanceof Double) {
                        return decimalFormat.format(o);
                    } else if (o instanceof Float) {
                        return decimalFormat.format(o);
                    } else {
                        return o;
                    }
                }
        ).toArray();
        return legacyComponentSerializer.deserialize(instance.getFormatted(key, args));
    }

    @Contract(pure = true)
    public static Component substitute(String key, Object... args) {
        if (instance == null) return Component.text("<Not initialized>");
        return legacyComponentSerializer.deserialize(instance.getSubstituted(key, args));
    }

    public static void sendSync(UUID playerId, String key, Object... args) {
        if (instance == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(Hmarket.getInstance(), () -> {
                    var player = Bukkit.getPlayer(playerId);
                    if (player == null) return;
                    HMI18n.send(player, key, args);
                }
        );
    }

    public static void sendSubstituteSync(UUID playerId, String key, Object... args) {
        if (instance == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(Hmarket.getInstance(), () -> {
                    var player = Bukkit.getPlayer(playerId);
                    if (player == null) return;
                    HMI18n.sendSubstitute(player, key, args);
                }
        );
    }


    public static void send(@NotNull CommandSender recipient, String key, Object... args) {
        if (recipient instanceof Player player && !player.isOnline()) {
            var aoMessage = AoMessage.getInstance();
            if (aoMessage != null) {
                aoMessage.sendMessageTo(player.getUniqueId(), format(key, args));
            }
        } else {
            recipient.sendMessage(format(key, args));
        }
    }

    public static void sendSubstitute(@NotNull CommandSender recipient, String key, Object... args) {
        if (recipient instanceof Player player && !player.isOnline()) {
            var aoMessage = AoMessage.getInstance();
            if (aoMessage != null) {
                aoMessage.sendMessageTo(player.getUniqueId(), substitute(key, args));
            }
        } else {
            recipient.sendMessage(substitute(key, args));
        }
    }


    @Override
    protected Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected String getLanguage() {
        return this.language;
    }
}
