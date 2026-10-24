package pl.sektor.core2;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GUI warpow. Nazwy warpow czyta z data.yml pluginu SektorCore (partia 1), teleport robi przez /warp. */
public class WarpMenu implements CommandExecutor {
    private final Core2 pl;

    public WarpMenu(Core2 pl) {
        this.pl = pl;
    }

    private List<String> warpNames() {
        File f = new File(pl.getDataFolder().getParentFile(), "SektorCore/data.yml");
        List<String> out = new ArrayList<>();
        if (!f.exists()) return out;
        ConfigurationSection s = YamlConfiguration.loadConfiguration(f).getConfigurationSection("warps");
        if (s != null) out.addAll(s.getKeys(false));
        Collections.sort(out);
        return out;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player p)) {
            Util.msg(sender, "&cTylko dla graczy.");
            return true;
        }
        open(p, 0);
        return true;
    }

    private void open(Player p, int page) {
        List<String> w = warpNames();
        int per = 28;
        int pages = Math.max(1, (int) Math.ceil(w.size() / (double) per));
        page = Math.max(0, Math.min(page, pages - 1));
        final int pg = page;
        Menu m = new Menu(54, "&8» &b&lWARPY &8« &7(" + (pg + 1) + "/" + pages + ")");
        m.border(Util.pane(Material.CYAN_STAINED_GLASS_PANE));
        if (w.isEmpty())
            m.set(22, Util.item(Material.BARRIER, "&cBrak warpow", "&7Administrator jeszcze ich nie ustawil."));
        for (int i = 0; i < per; i++) {
            int idx = pg * per + i;
            if (idx >= w.size()) break;
            String name = w.get(idx);
            int slot = (1 + i / 7) * 9 + 1 + i % 7;
            Material mat = Material.matchMaterial(pl.getConfig().getString("warp-icons." + name, "ENDER_PEARL"));
            if (mat == null) mat = Material.ENDER_PEARL;
            m.set(slot, Util.item(mat, "&b&l" + name.toUpperCase(), "", "&aKliknij, aby sie teleportowac"), e -> {
                p.closeInventory();
                Bukkit.getScheduler().runTask(pl, () -> p.performCommand("warp " + name));
            });
        }
        if (pg > 0) m.set(48, Util.item(Material.ARROW, "&e&l« Poprzednia"), e -> pl.later(() -> open(p, pg - 1)));
        if (pg < pages - 1) m.set(50, Util.item(Material.ARROW, "&e&lNastepna »"), e -> pl.later(() -> open(p, pg + 1)));
        m.set(49, Util.item(Material.BARRIER, "&c&lZamknij"), e -> p.closeInventory());
        m.open(p);
    }
}
