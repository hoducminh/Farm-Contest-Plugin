package com.farmcontest.contest;

import com.farmcontest.ConfigManager;
import com.farmcontest.DataManager;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class FarmClassicMode implements ContestMode {

    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final RewardDistributor rewardDistributor;
    private final Random random = new Random();

    public FarmClassicMode(ConfigManager configManager, DataManager dataManager, RewardDistributor rewardDistributor) {
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.rewardDistributor = rewardDistributor;
    }

    @Override
    public String id() {
        return "farm-classic";
    }

    @Override
    public void onStart(ContestContext ctx) {
        dataManager.clearContestData();
        ctx.selectedCrops = selectRandomCrops();

        if (configManager.isLuckyCropEnabled() && !ctx.selectedCrops.isEmpty()) {
            List<Material> pool = new ArrayList<>(ctx.selectedCrops);
            ctx.luckyCrop = pool.get(random.nextInt(pool.size()));
        } else {
            ctx.luckyCrop = null;
        }
    }

    @Override
    public void onPointSource(ContestContext ctx, UUID player, int points) {
    }

    @Override
    public void onEnd(ContestContext ctx) {
        rewardDistributor.distributeIndividual();
        if (configManager.shouldResetPointsAfter()) dataManager.clearContestData();
    }

    private Set<Material> selectRandomCrops() {
        Map<Material, Double> cropChances = configManager.getCropChances();
        int count = configManager.getCropSelectionCount();
        List<Material> available = new ArrayList<>(cropChances.keySet());
        Set<Material> selected = new HashSet<>();

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            double total = available.stream().mapToDouble(cropChances::get).sum();
            double roll = random.nextDouble() * total;
            double acc = 0;
            for (Material crop : available) {
                acc += cropChances.get(crop);
                if (roll <= acc) {
                    selected.add(crop);
                    available.remove(crop);
                    break;
                }
            }
        }
        return selected;
    }
}
