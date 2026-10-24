package pl.sektor.core2;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

/** /keyall - odliczanie, potem kazdy online dostaje przedmiot do /odbierz. */
public class KeyAll implements CommandExecutor {
    private final Core2 pl;
    private boolean running = false;

    public KeyAll(Core2 pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("sektor.admin")) {
            Util.msg(s, "&cBrak uprawnien.");
            return true;
        }
        if (!(s instanceof Player p)) {
            Util.msg(s, "&cTylko gracz (przedmiot z reki).");
            return true;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (Util.empty(hand)) {
            Util.msg(s, "&cWez do reki przedmiot, ktory ma byc rozdany.");
            return true;
        }
        if (running) {
            Util.msg(s, "&cKeyall juz trwa.");
            return true;
        }
        ItemStack reward = hand.clone();
        String label2 = a.length > 0 ? String.join(" ", a) : reward.getAmount() + "x " + Util.name(reward);
        start(reward, label2);
        return true;
    }

    private void start(ItemStack reward, String label) {
        running = true;
        final int total = Math.max(1, pl.getConfig().getInt("keyall.countdown", 10));
        final BossBar bar = BossBar.bossBar(Util.c("&6&l⚡ KEYALL"), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        Bukkit.broadcast(Util.c(Util.prefix + "&6&l⚡ KEYALL &7za &e&l" + total + " sekund&7! Nagroda: &f" + label));

        new BukkitRunnable() {
            int sec = total;

            @Override
            public void run() {
                if (sec > 0) {
                    bar.name(Util.c("&6&l⚡ KEYALL &7za &e&l" + sec + "s &8• &f" + label));
                    bar.progress(Math.max(0f, Math.min(1f, sec / (float) total)));
                    Title t = Title.title(Util.c("&6&l⚡ KEYALL ⚡"),
                            Util.c("&7za &e&l" + sec + " &7sekund &8• &f" + label),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO));
                    for (Player pp : Bukkit.getOnlinePlayers()) {
                        pp.showBossBar(bar);
                        pp.showTitle(t);
                        pp.playSound(pp.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, sec <= 3 ? 1.8f : 1.0f);
                    }
                    sec--;
                } else {
                    cancel();
                    finish(bar, reward, label);
                }
            }
        }.runTaskTimer(pl, 0L, 20L);
    }

    private void finish(BossBar bar, ItemStack reward, String label) {
        Title t = Title.title(Util.c("&a&l✔ KEYALL!"), Util.c("&7Wpisz &a&l/odbierz"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(3), Duration.ofMillis(500)));
        for (Player pp : Bukkit.getOnlinePlayers()) {
            pp.hideBossBar(bar);
            pl.pending.add(pp.getUniqueId(), reward.clone());
        }
        pl.data.save();
        for (Player pp : Bukkit.getOnlinePlayers()) {
            pl.pending.refresh(pp);
            pp.showTitle(t);
            pp.playSound(pp.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        Bukkit.broadcast(Util.c("&a&l✔ KEYALL &7- kazdy gracz online dostal: &f" + label + " &8| &7Wpisz &a&l/odbierz"));
        running = false;
    }
}
