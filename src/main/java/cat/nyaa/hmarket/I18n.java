package cat.nyaa.hmarket;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;


public class I18n extends LanguageRepository {
    @Nullable
    private static I18n instance = null;
    private final Hmarket plugin;
    private final String lang;

    public I18n(Hmarket plugin, String lang) {
        instance = this;
        this.plugin = plugin;
        this.lang = lang;
        load();
    }
    @Contract(pure = true)
    public static String format(String key, Object... args) {
        if (instance == null) return "<Not initialized>";
        return instance.getFormatted(key, args);
    }

    public static String substitute(String key, Object... args) {
        if (instance == null) return "<Not initialized>";
        return instance.getSubstituted(key, args);
    }

    public static void send(CommandSender recipient, String key, Object... args) {
        recipient.sendMessage(format(key, args));
    }


    @Override
    protected Plugin getPlugin() {
        return plugin;
    }

    @Override
    protected String getLanguage() {
        return lang;
    }
}
