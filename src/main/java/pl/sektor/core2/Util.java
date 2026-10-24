package pl.sektor.core2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Util {
    private static final LegacyComponentSerializer LEG = LegacyComponentSerializer.legacyAmpersand();
    public static String prefix = "";

    private Util() {}

    public static Component c(String s) {
        return LEG.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    public static void msg(CommandSender s, String m) {
        s.sendMessage(c(prefix + m));
    }

    public static ItemStack item(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.displayName(c(name));
        if (lore.length > 0) meta.lore(Arrays.stream(lore).map(Util::c).toList());
        i.setItemMeta(meta);
        return i;
    }

    public static ItemStack pane(Material m) {
        return item(m, " ");
    }

    public static boolean empty(ItemStack i) {
        return i == null || i.getType().isAir();
    }

    public static String name(ItemStack i) {
        ItemMeta m = i.getItemMeta();
        if (m != null && m.hasDisplayName() && m.displayName() != null)
            return PlainTextComponentSerializer.plainText().serialize(m.displayName());
        return i.getType().name().toLowerCase().replace('_', ' ');
    }

    /** Dodaje item do ekwipunku, zwraca to co sie nie zmiescilo. */
    public static List<ItemStack> addOrReturn(Player p, ItemStack st) {
        return new ArrayList<>(p.getInventory().addItem(st.clone()).values());
    }

    public static String time(long sec) {
        long d = sec / 86400, h = sec % 86400 / 3600, m = sec % 3600 / 60, s = sec % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s");
        return sb.toString().trim();
    }
}
