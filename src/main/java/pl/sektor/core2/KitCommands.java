package pl.sektor.core2;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KitCommands implements CommandExecutor, TabCompleter {
    private final Core2 pl;

    public KitCommands(Core2 pl) {
        this.pl = pl;
    }

    private List<String> names() {
        ConfigurationSection s = pl.data.cfg.getConfigurationSection("kits");
        List<String> l = s == null ? new ArrayList<>() : new ArrayList<>(s.getKeys(false));
        Collections.sort(l);
        return l;
    }

    private boolean canUse(Player p, String n) {
        return !pl.getConfig().getBoolean("kits-require-permission")
                || p.hasPermission("sektor.kit." + n) || p.hasPermission("sektor.kit.*");
    }

    private long remaining(Player p, String n) {
        long cd = pl.data.cfg.getLong("kits." + n + ".cooldown");
        long last = pl.data.cfg.getLong("kitcd." + p.getUniqueId() + "." + n, 0);
        long left = (last + cd * 1000 - System.currentTimeMillis() + 999) / 1000;
        return Math.max(0, left);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (cmd.getName().equalsIgnoreCase("kitadmin")) return admin(s, a);
        if (!(s instanceof Player p)) {
            Util.msg(s, "&cTylko dla graczy.");
            return true;
        }
        if (a.length == 0) menu(p);
        else claim(p, a[0].toLowerCase());
        return true;
    }

    // ---------- odbieranie ----------
    private void claim(Player p, String n) {
        String base = "kits." + n;
        if (!pl.data.cfg.contains(base)) {
            Util.msg(p, "&cNie ma takiego zestawu.");
            return;
        }
        if (!canUse(p, n)) {
            Util.msg(p, "&cNie masz uprawnien do tego zestawu.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }
        long left = remaining(p, n);
        if (left > 0 && !p.hasPermission("sektor.bypass.cooldown")) {
            Util.msg(p, "&cZestaw bedzie dostepny za &f" + Util.time(left));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }
        for (ItemStack it : pl.data.items(base + ".items"))
            for (ItemStack l : Util.addOrReturn(p, it))
                p.getWorld().dropItemNaturally(p.getLocation(), l);
        pl.data.cfg.set("kitcd." + p.getUniqueId() + "." + n, System.currentTimeMillis());
        pl.data.save();
        Util.msg(p, "&aOdebrano zestaw &6&l" + n.toUpperCase() + "&a!");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
    }

    // ---------- GUI ----------
    private ItemStack icon(Player p, String n) {
        String base = "kits." + n;
        Material mat = Material.matchMaterial(pl.data.cfg.getString(base + ".icon", "CHEST"));
        if (mat == null) mat = Material.CHEST;
        long left = remaining(p, n);
        String status = !canUse(p, n) ? "&c✘ Brak uprawnien"
                : left > 0 ? "&c⏳ Za &f" + Util.time(left) : "&a✔ Dostepny";
        return Util.item(mat, "&6&l" + n.toUpperCase(),
                "&7Odswiezanie: &f" + Util.time(pl.data.cfg.getLong(base + ".cooldown")),
                "&7Przedmiotow: &f" + pl.data.items(base + ".items").size(),
                "", status, "", "&aLPM &7- odbierz", "&ePPM &7- podglad");
    }

    private void menu(Player p) {
        List<String> names = names();
        int rows = Math.min(6, Math.max(3, (int) Math.ceil(names.size() / 7.0) + 2));
        Menu m = new Menu(rows * 9, "&8» &6&lZESTAWY &8«");
        m.border(Util.pane(Material.GRAY_STAINED_GLASS_PANE));
        int cap = (rows - 2) * 7;
        for (int i = 0; i < names.size() && i < cap; i++) {
            String n = names.get(i);
            int slot = (1 + i / 7) * 9 + 1 + i % 7;
            m.set(slot, icon(p, n), e -> {
                if (e.isRightClick()) pl.later(() -> preview(p, n));
                else {
                    claim(p, n);
                    pl.later(() -> menu(p));
                }
            });
        }
        if (names.isEmpty())
            m.set(13, Util.item(Material.BARRIER, "&cBrak zestawow", "&7Administrator jeszcze ich nie dodal."));
        m.set((rows - 1) * 9 + 4, Util.item(Material.BARRIER, "&c&lZamknij"), e -> p.closeInventory());
        m.open(p);
    }

    private void preview(Player p, String n) {
        List<ItemStack> items = pl.data.items("kits." + n + ".items");
        int rows = Math.min(6, Math.max(2, (int) Math.ceil(items.size() / 9.0) + 1));
        Menu m = new Menu(rows * 9, "&8» &6Podglad: &e" + n);
        for (int i = 0; i < items.size() && i < (rows - 1) * 9; i++) m.set(i, items.get(i).clone());
        for (int i = (rows - 1) * 9; i < rows * 9; i++) m.set(i, Util.pane(Material.GRAY_STAINED_GLASS_PANE));
        m.set((rows - 1) * 9 + 4, Util.item(Material.ARROW, "&e&l« Wroc"), e -> pl.later(() -> menu(p)));
        m.open(p);
    }

    // ---------- admin ----------
    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("sektor.admin")) {
            Util.msg(s, "&cBrak uprawnien.");
            return true;
        }
        if (a.length == 0) {
            Util.msg(s, "&e/kitadmin utworz <nazwa> <cooldown_s> &7- z Twojego ekwipunku");
            Util.msg(s, "&e/kitadmin ikona <nazwa> &7- ikona z przedmiotu w rece");
            Util.msg(s, "&e/kitadmin cooldown <nazwa> <s> &8| &e/kitadmin usun <nazwa> &8| &e/kitadmin lista");
            return true;
        }
        String sub = a[0].toLowerCase();
        if (sub.equals("lista")) {
            Util.msg(s, "&7Zestawy: &f" + (names().isEmpty() ? "brak" : String.join("&7, &f", names())));
            return true;
        }
        if (a.length < 2) {
            Util.msg(s, "&cPodaj nazwe zestawu.");
            return true;
        }
        String n = a[1].toLowerCase();
        String base = "kits." + n;
        switch (sub) {
            case "utworz" -> {
                if (!(s instanceof Player p)) { Util.msg(s, "&cTylko gracz."); return true; }
                if (!n.matches("[a-z0-9_\\-]{1,16}")) { Util.msg(s, "&cNazwa: a-z, 0-9, _ , - (max 16)."); return true; }
                long cd = 0;
                if (a.length > 2) {
                    try { cd = Long.parseLong(a[2]); } catch (NumberFormatException ex) { Util.msg(s, "&cCooldown w sekundach."); return true; }
                }
                List<ItemStack> items = new ArrayList<>();
                for (ItemStack it : p.getInventory().getContents()) if (!Util.empty(it)) items.add(it.clone());
                if (items.isEmpty()) { Util.msg(s, "&cMasz pusty ekwipunek."); return true; }
                pl.data.cfg.set(base + ".cooldown", cd);
                pl.data.cfg.set(base + ".icon", items.get(0).getType().name());
                pl.data.setItems(base + ".items", items);
                pl.data.save();
                Util.msg(s, "&aUtworzono zestaw &6" + n + " &7(" + items.size() + " przedmiotow, cooldown " + Util.time(cd) + ")");
            }
            case "usun" -> {
                if (!pl.data.cfg.contains(base)) { Util.msg(s, "&cNie ma takiego zestawu."); return true; }
                pl.data.cfg.set(base, null);
                pl.data.save();
                Util.msg(s, "&aUsunieto zestaw &6" + n);
            }
            case "ikona" -> {
                if (!(s instanceof Player p)) { Util.msg(s, "&cTylko gracz."); return true; }
                ItemStack h = p.getInventory().getItemInMainHand();
                if (!pl.data.cfg.contains(base) || Util.empty(h)) { Util.msg(s, "&cZestaw nie istnieje albo pusta reka."); return true; }
                pl.data.cfg.set(base + ".icon", h.getType().name());
                pl.data.save();
                Util.msg(s, "&aUstawiono ikone.");
            }
            case "cooldown" -> {
                if (!pl.data.cfg.contains(base) || a.length < 3) { Util.msg(s, "&c/kitadmin cooldown <nazwa> <s>"); return true; }
                try { pl.data.cfg.set(base + ".cooldown", Long.parseLong(a[2])); } catch (NumberFormatException ex) { Util.msg(s, "&cLiczba sekund."); return true; }
                pl.data.save();
                Util.msg(s, "&aUstawiono cooldown.");
            }
            default -> Util.msg(s, "&cNieznana podkomenda.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("kitadmin")) {
            if (a.length == 1) out.addAll(List.of("utworz", "usun", "ikona", "cooldown", "lista"));
            else if (a.length == 2) out.addAll(names());
        } else if (a.length == 1) out.addAll(names());
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
