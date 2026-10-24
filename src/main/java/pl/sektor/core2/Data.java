package pl.sektor.core2;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Data {
    private final Core2 plugin;
    private final File file;
    public final YamlConfiguration cfg;

    public Data(Core2 plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie mozna zapisac data.yml: " + e.getMessage());
        }
    }

    public List<ItemStack> items(String path) {
        List<ItemStack> out = new ArrayList<>();
        List<?> raw = cfg.getList(path);
        if (raw == null) return out;
        for (Object o : raw) if (o instanceof ItemStack i) out.add(i);
        return out;
    }

    public void setItems(String path, List<ItemStack> list) {
        cfg.set(path, list.isEmpty() ? null : list);
    }
}
