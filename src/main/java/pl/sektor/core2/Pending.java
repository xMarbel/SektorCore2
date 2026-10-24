package pl.sektor.core2;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Nagrody czekajace na /odbierz + zielony bossbar. */
public class Pending implements CommandExecutor, Listener {
    private final Core2 pl;
    private final BossBar bar = BossBar.bossBar(
            Util.c("&a&l✔ Masz nagrody do odebrania! &7Wpisz &a&l/odbierz"),
            1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private boolean alt;

    public Pending(Core2 pl) {
        this.pl = pl;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, () -> {
            alt = !alt;
            bar.name(Util.c(alt
                    ? "&a&l✔ Masz nagrody do odebrania! &7Wpisz &a&l/odbierz"
                    : "&2&l✔ Masz nagrody do odebrania! &7Wpisz &2&l/odbierz"));
        }, 20L, 20L);
    }

    public List<ItemStack> get(UUID id) {
        return pl.data.items("pending." + id);
    }

    public void add(UUID id, ItemStack item) {
        List<ItemStack> l = get(id);
        l.add(item);
        pl.data.setItems("pending." + id, l);
    }

    public void refresh(Player p) {
        if (get(p.getUniqueId()).isEmpty()) p.hideBossBar(bar);
        else p.showBossBar(bar);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(pl, () -> {
            if (!p.isOnline()) return;
            refresh(p);
            if (!get(p.getUniqueId()).isEmpty())
                Util.msg(p, "&aMasz nagrody do odebrania! Wpisz &l/odbierz");
        }, 30L);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!(s instanceof Player p)) {
            Util.msg(s, "&cTylko dla graczy.");
            return true;
        }
        List<ItemStack> list = get(p.getUniqueId());
        if (list.isEmpty()) {
            Util.msg(p, "&cNie masz nic do odebrania.");
            return true;
        }
        List<ItemStack> left = new ArrayList<>();
        Map<String, Integer> got = new LinkedHashMap<>();
        Map<String, Integer> notFit = new LinkedHashMap<>();
        for (ItemStack it : list) {
            List<ItemStack> back = Util.addOrReturn(p, it);
            int rest = 0;
            for (ItemStack b : back) {
                left.add(b);
                rest += b.getAmount();
                notFit.merge(Util.name(b), b.getAmount(), Integer::sum);
            }
            int given = it.getAmount() - rest;
            if (given > 0) got.merge(Util.name(it), given, Integer::sum);
        }
        pl.data.setItems("pending." + p.getUniqueId(), left);
        pl.data.save();
        refresh(p);
        if (!got.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            got.forEach((n, c) -> sb.append("&f").append(c).append("x ").append(n).append("&7, "));
            Util.msg(p, "&a&l✔ Odebrano: " + sb.substring(0, sb.length() - 2));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        }
        if (!left.isEmpty()) {
            Util.msg(p, "&eNie zmiescilo sie w ekwipunku (" + notFit.size()
                    + " rodz.). Zrob miejsce i wpisz &6/odbierz &eponownie.");
            if (got.isEmpty()) p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
        }
        return true;
    }
}
