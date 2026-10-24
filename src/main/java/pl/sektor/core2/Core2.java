package pl.sektor.core2;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Core2 extends JavaPlugin {
    public Data data;
    public Pending pending;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Util.prefix = getConfig().getString("prefix", "&7");
        data = new Data(this);
        pending = new Pending(this);
        KitCommands kits = new KitCommands(this);
        Roulette roulette = new Roulette(this);
        reg("kit", kits);
        reg("kitadmin", kits);
        reg("warpy", new WarpMenu(this));
        reg("losuj-zestaw", roulette);
        reg("losujadmin", roulette);
        reg("keyall", new KeyAll(this));
        reg("odbierz", pending);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new Menu.Events(), this);
        pm.registerEvents(pending, this);
        pending.start();
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }

    public void later(Runnable r) {
        getServer().getScheduler().runTask(this, r);
    }

    private void reg(String name, CommandExecutor ex) {
        PluginCommand c = getCommand(name);
        if (c == null) {
            getLogger().warning("Brak komendy w plugin.yml: " + name);
            return;
        }
        c.setExecutor(ex);
        if (ex instanceof TabCompleter t) c.setTabCompleter(t);
    }
}
