package pl.sektor.core2;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

/** /losuj-zestaw (animacja w GUI + tytul na srodku ekranu) i /losujadmin. */
public class Roulette implements CommandExecutor, TabCompleter {
    private final Core2 pl;
    private final Set<UUID> rolling = new HashSet<>();
    private final Random rnd = new Random();

    public Roulette(Core2 pl) {
        this.pl = pl;
    }

    private record R(List<ItemStack> items, List<Integer> w) {}

    private R load() {
        List<ItemStack> items = pl.data.items("roulette.items");
        List<Integer> w = new ArrayList<>(pl.data.cfg.getIntegerList("roulette.weights"));
        int n = Math.min(items.size(), w.size());
        while (items.size() > n) items.remove(items.size() - 1);
        while (w.size() > n) w.remove(w.size() - 1);
        return new R(items, w);
    }

    private void saveR(R r) {
        pl.data.setItems("roulette.items", r.items());
        pl.data.cfg.set("roulette.weights", r.w().isEmpty() ? null : r.w());
        pl.data.save();
    }

    private int pick(List<Integer> w) {
        int total = 0;
        for (int x : w) total += x;
        int r = rnd.nextInt(total);
        for (int i = 0; i < w.size(); i++) {
            r -= w.get(i);
            if (r < 0) return i;
        }
        return w.size() - 1;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (cmd.getName().equalsIgnoreCase("losujadmin")) return admin(s, a);
        if (!(s instanceof Player p)) {
            Util.msg(s, "&cTylko dla graczy.");
            return true;
        }
        roll(p);
        return true;
    }

    // ---------- losowanie ----------
    private void roll(Player p) {
        UUID id = p.getUniqueId();
        if (rolling.contains(id)) {
            Util.msg(p, "&cJuz losujesz!");
            return;
        }
        R r = load();
        if (r.items().isEmpty()) {
            Util.msg(p, "&cAdministracja nie dodala jeszcze zadnych nagrod.");
            return;
        }
        long now = System.currentTimeMillis();
        long cd = pl.data.cfg.getLong("roulette.cooldown", 3600);
        long left = (pl.data.cfg.getLong("rcd." + id, 0) + cd * 1000 - now + 999) / 1000;
        if (left > 0 && !p.hasPermission("sektor.bypass.cooldown")) {
            Util.msg(p, "&cNastepne losowanie za &f" + Util.time(left));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }
        pl.data.cfg.set("rcd." + id, now);
        pl.data.save();
        rolling.add(id);

        final int steps = pl.getConfig().getInt("roulette.steps", 40);
        final int dur = Math.max(20, pl.getConfig().getInt("roulette.duration-ticks", 100));
        final ItemStack win = r.items().get(pick(r.w())).clone();
        final List<ItemStack> strip = new ArrayList<>();
        for (int i = 0; i < steps + 9; i++) strip.add(r.items().get(pick(r.w())).clone());
        strip.set(steps + 4, win.clone());

        Menu m = new Menu(27, "&8» &6&lLOSOWANIE &8«");
        decorate(m.getInventory());
        render(m.getInventory(), strip, 0);
        m.open(p);
        p.showTitle(Title.title(Util.c("&6&l✦ LOSOWANIE ✦"), Util.c("&7Trwa losowanie nagrody..."),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(dur * 50L), Duration.ofMillis(200))));

        new BukkitRunnable() {
            int t = 0, last = -1;

            @Override
            public void run() {
                t++;
                double x = Math.min(1.0, (double) t / dur);
                int pos = (int) Math.round(steps * (1 - Math.pow(1 - x, 3)));
                if (pos != last) {
                    last = pos;
                    render(m.getInventory(), strip, pos);
                    if (p.isOnline())
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.0f + 0.8f * pos / steps);
                }
                if (t >= dur) {
                    cancel();
                    Bukkit.getScheduler().runTaskLater(pl, () -> finish(id, win), 30L);
                }
            }
        }.runTaskTimer(pl, 1L, 1L);
    }

    private void decorate(Inventory inv) {
        for (int i = 0; i < 27; i++) inv.setItem(i, Util.pane(Material.GRAY_STAINED_GLASS_PANE));
        for (int i : new int[]{3, 5, 21, 23}) inv.setItem(i, Util.pane(Material.YELLOW_STAINED_GLASS_PANE));
        inv.setItem(4, Util.item(Material.HOPPER, "&e&l▼"));
        inv.setItem(22, Util.item(Material.END_ROD, "&e&l▲"));
    }

    private void render(Inventory inv, List<ItemStack> strip, int off) {
        for (int k = 0; k < 9; k++) inv.setItem(9 + k, strip.get(off + k).clone());
    }

    private void finish(UUID id, ItemStack win) {
        rolling.remove(id);
        String label = win.getAmount() + "x " + Util.name(win);
        Player p = Bukkit.getPlayer(id);
        if (p == null) {
            pl.pending.add(id, win);
            pl.data.save();
        } else {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
            List<ItemStack> left = Util.addOrReturn(p, win);
            for (ItemStack l : left) pl.pending.add(id, l);
            if (!left.isEmpty()) {
                pl.data.save();
                pl.pending.refresh(p);
                Util.msg(p, "&eEkwipunek pelny - reszta nagrody czeka pod &6/odbierz&e.");
            }
            p.showTitle(Title.title(Util.c("&a&l✔ WYLOSOWANO!"), Util.c("&f" + label),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        String who = p != null ? p.getName() : Bukkit.getOfflinePlayer(id).getName();
        Bukkit.broadcast(Util.c("&6&l[LOSOWANIE] &e" + who + " &7wylosowal &f" + label + "&7!"));
    }

    // ---------- admin ----------
    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("sektor.admin")) {
            Util.msg(s, "&cBrak uprawnien.");
            return true;
        }
        if (a.length == 0) {
            Util.msg(s, "&e/losujadmin dodaj <szansa> &7- przedmiot z reki");
            Util.msg(s, "&e/losujadmin usun <nr> &8| &e/losujadmin lista &8| &e/losujadmin cooldown <s>");
            return true;
        }
        R r = load();
        switch (a[0].toLowerCase()) {
            case "dodaj" -> {
                if (!(s instanceof Player p)) { Util.msg(s, "&cTylko gracz."); return true; }
                ItemStack h = p.getInventory().getItemInMainHand();
                if (Util.empty(h) || a.length < 2) { Util.msg(s, "&c/losujadmin dodaj <szansa> (przedmiot w rece)"); return true; }
                int w;
                try { w = Integer.parseInt(a[1]); } catch (NumberFormatException e) { w = 0; }
                if (w < 1) { Util.msg(s, "&cSzansa to liczba >= 1 (waga)."); return true; }
                r.items().add(h.clone());
                r.w().add(w);
                saveR(r);
                Util.msg(s, "&aDodano &f" + h.getAmount() + "x " + Util.name(h) + " &7(waga " + w + ") pod numerem &e" + r.items().size());
            }
            case "usun" -> {
                int i;
                try { i = Integer.parseInt(a[1]) - 1; } catch (Exception e) { i = -1; }
                if (i < 0 || i >= r.items().size()) { Util.msg(s, "&cZly numer."); return true; }
                r.items().remove(i);
                r.w().remove(i);
                saveR(r);
                Util.msg(s, "&aUsunieto.");
            }
            case "cooldown" -> {
                try { pl.data.cfg.set("roulette.cooldown", Long.parseLong(a[1])); } catch (Exception e) { Util.msg(s, "&c/losujadmin cooldown <sekundy>"); return true; }
                pl.data.save();
                Util.msg(s, "&aCooldown ustawiony.");
            }
            case "lista" -> {
                if (s instanceof Player p) adminMenu(p);
                else {
                    int total = r.w().stream().mapToInt(Integer::intValue).sum();
                    for (int i = 0; i < r.items().size(); i++)
                        Util.msg(s, "&e" + (i + 1) + ". &f" + r.items().get(i).getAmount() + "x " + Util.name(r.items().get(i))
                                + " &7waga " + r.w().get(i) + " (" + pct(r.w().get(i), total) + "%)");
                }
            }
            default -> Util.msg(s, "&cNieznana podkomenda.");
        }
        return true;
    }

    private String pct(int w, int total) {
        return String.format(Locale.US, "%.1f", total == 0 ? 0.0 : 100.0 * w / total);
    }

    private void adminMenu(Player p) {
        R r = load();
        int total = r.w().stream().mapToInt(Integer::intValue).sum();
        Menu m = new Menu(54, "&8» &6Nagrody losowania &8«");
        for (int i = 45; i < 54; i++) m.set(i, Util.pane(Material.GRAY_STAINED_GLASS_PANE));
        for (int i = 0; i < r.items().size() && i < 45; i++) {
            final int idx = i;
            ItemStack it = r.items().get(i).clone();
            ItemMeta meta = it.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() && meta.lore() != null
                    ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Util.c(""));
            lore.add(Util.c("&7Waga: &f" + r.w().get(i) + " &8(" + pct(r.w().get(i), total) + "%)"));
            lore.add(Util.c("&cSHIFT+PPM &7- usun"));
            meta.lore(lore);
            it.setItemMeta(meta);
            m.set(i, it, e -> {
                if (e.isShiftClick() && e.isRightClick()) {
                    R cur = load();
                    if (idx < cur.items().size()) {
                        cur.items().remove(idx);
                        cur.w().remove(idx);
                        saveR(cur);
                    }
                    pl.later(() -> adminMenu(p));
                }
            });
        }
        m.set(49, Util.item(Material.BARRIER, "&c&lZamknij"), e -> p.closeInventory());
        m.open(p);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("losujadmin") && a.length == 1)
            out.addAll(List.of("dodaj", "usun", "lista", "cooldown"));
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.startsWith(last));
        return out;
    }
}
