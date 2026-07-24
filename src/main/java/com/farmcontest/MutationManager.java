package com.farmcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class MutationManager {

    private final FarmContest plugin;
    private File file;
    private FileConfiguration config;

    private final NamespacedKey keyMutationType;
    private final NamespacedKey keyMultiplier;
    private final NamespacedKey keyWeight;
    private final NamespacedKey keyOwner;

    private final Random random = new Random();

    public MutationManager(FarmContest plugin) {
        this.plugin = plugin;
        this.keyMutationType = new NamespacedKey(plugin, "mutation_type");
        this.keyMultiplier = new NamespacedKey(plugin, "mutation_multiplier");
        this.keyWeight = new NamespacedKey(plugin, "mutation_weight");
        this.keyOwner = new NamespacedKey(plugin, "mutation_owner");

        loadConfig();
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder(), "mutation.yml");
        if (!file.exists()) {
            plugin.saveResource("mutation.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
    public void tryCreateMutation(Player player, Block block, Material cropMaterial) {
        if (!config.getBoolean("settings.enabled", true)) return;

        double globalChance = config.getDouble("settings.global_chance", 0.03);
        if (random.nextDouble() > globalChance) return;

        ConfigurationSection mutationsSec = config.getConfigurationSection("mutations");
        if (mutationsSec == null) return;

        List<String> validMutations = new ArrayList<>();
        for (String mutationKey : mutationsSec.getKeys(false)) {
            List<String> availableCrops = mutationsSec.getStringList(mutationKey + ".available_crops");
            if (availableCrops.contains(cropMaterial.name())) {
                String condition = mutationsSec.getString(mutationKey + ".condition", "");
                if (checkCondition(player, condition)) {
                    validMutations.add(mutationKey);
                }
            }
        }

        if (validMutations.isEmpty()) return;

        String selectedMutation = validMutations.get(random.nextInt(validMutations.size()));
        ItemStack mutatedItem = createMutatedItemStack(player, cropMaterial, selectedMutation);

        Location loc = block.getLocation();
        if (loc.getWorld() != null) {
            loc.getWorld().dropItemNaturally(loc, mutatedItem);

            String mutationName = config.getString("mutations." + selectedMutation + ".display", selectedMutation);
            double multiplier = config.getDouble("mutations." + selectedMutation + ".multiplier", 1.0);

            String msg = plugin.getConfigManager().getMutationBroadcastMessage();
            msg = msg.replace("{mutation}", mutationName)
                     .replace("{crop}", cropMaterial.name())
                     .replace("{multiplier}", String.valueOf(multiplier));

            player.sendMessage(parseColor(msg));
        }
    }

    public boolean checkCondition(Player player, String condition) {
        if (condition == null || condition.isEmpty()) return true;

        World world = player.getWorld();
        long time = world.getTime();

        if (condition.equalsIgnoreCase("rain")) {
            return world.hasStorm() && !world.isThundering();
        } else if (condition.equalsIgnoreCase("storm")) {
            return world.isThundering();
        } else if (condition.equalsIgnoreCase("midday")) {
            return time >= 5000 && time <= 7000 && !world.hasStorm();
        } else if (condition.equalsIgnoreCase("midnight")) {
            return time >= 13000 && time <= 23000;
        } else if (condition.toLowerCase().startsWith("biome:")) {
            String targetBiomeStr = condition.substring(6).toUpperCase();
            try {
                Biome targetBiome = Biome.valueOf(targetBiomeStr);
                Biome playerBiome = player.getLocation().getBlock().getBiome();
                return playerBiome.equals(targetBiome);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    public ItemStack createMutatedItemStack(Player player, Material cropMaterial, String mutationKey) {
        ItemStack item = new ItemStack(cropMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        ConfigurationSection sec = config.getConfigurationSection("mutations." + mutationKey);

        String defaultName = plugin.getConfigManager().getMutationDefaultName();
        String defaultDisplay = plugin.getConfigManager().getMutationDefaultDisplay();
        String mutationName = sec != null ? sec.getString("name", defaultName) : defaultName;
        String mutationDisplay = sec != null ? sec.getString("display", defaultDisplay) : defaultDisplay;
        double multiplier = sec != null ? sec.getDouble("multiplier", 1.5) : 1.5;

        double minWeight = config.getDouble("settings.weight.min", 10.0);
        double maxWeight = config.getDouble("settings.weight.max", 200.0);
        double weight = Math.round((minWeight + (maxWeight - minWeight) * random.nextDouble()) * 100.0) / 100.0;

        String ownerName = player.getName();
        String uuidStr = player.getUniqueId().toString();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyMutationType, PersistentDataType.STRING, mutationKey);
        pdc.set(keyMultiplier, PersistentDataType.DOUBLE, multiplier);
        pdc.set(keyWeight, PersistentDataType.DOUBLE, weight);
        pdc.set(keyOwner, PersistentDataType.STRING, uuidStr);

        String nameFormat = plugin.getConfigManager().getMutationItemNameFormat();
        String cropDisplayName = formatCropName(cropMaterial);

        nameFormat = nameFormat.replace("{mutation_name}", mutationName)
                               .replace("{crop_display}", cropDisplayName);

        meta.displayName(parseColor(nameFormat));

        List<String> rawLore = plugin.getConfigManager().getMutationItemLore();
        List<Component> loreComponents = new ArrayList<>();
        for (String line : rawLore) {
            String processed = line.replace("{mutation_name}", mutationName)
                                   .replace("{mutation_display}", mutationDisplay)
                                   .replace("{crop_display}", cropDisplayName)
                                   .replace("{multiplier}", String.valueOf(multiplier))
                                   .replace("{weight}", String.valueOf(weight))
                                   .replace("{owner}", ownerName);
            loreComponents.add(parseColor(processed));
        }
        meta.lore(loreComponents);
        item.setItemMeta(meta);

        return item;
    }

    public boolean isMutatedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(keyMutationType, PersistentDataType.STRING) && pdc.has(keyWeight, PersistentDataType.DOUBLE);
    }

    public double getItemValue(ItemStack item) {
        if (!isMutatedItem(item)) return 0.0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        double weight = pdc.getOrDefault(keyWeight, PersistentDataType.DOUBLE, 10.0);
        double multiplier = pdc.getOrDefault(keyMultiplier, PersistentDataType.DOUBLE, 1.0);
        double basePricePerKg = config.getDouble("shop.base_price_per_kg", 50.0);

        return weight * multiplier * basePricePerKg;
    }

    private String formatCropName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private Component parseColor(String legacyAmpersandText) {
        String text = legacyAmpersandText == null ? "" : legacyAmpersandText;
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }
}