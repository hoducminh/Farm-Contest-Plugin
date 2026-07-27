package com.farmcontest.contest;

import com.farmcontest.ConfigManager;
import com.farmcontest.DataManager;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MobHuntMode implements ContestMode {

    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final RewardDistributor rewardDistributor;
    private final Random random = new Random();

    public MobHuntMode(ConfigManager configManager, DataManager dataManager, RewardDistributor rewardDistributor) {
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.rewardDistributor = rewardDistributor;
    }

    @Override
    public String id() {
        return "mob-hunt";
    }

    @Override
    public void onStart(ContestContext ctx) {
        dataManager.clearContestData();
        ctx.selectedMobs = selectRandomMobs();
        ctx.luckyCrop = null;

        if (configManager.isLuckyMobEnabled() && !ctx.selectedMobs.isEmpty()) {
            List<EntityType> pool = new ArrayList<>(ctx.selectedMobs);
            ctx.luckyMob = pool.get(random.nextInt(pool.size()));
        } else {
            ctx.luckyMob = null;
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

    private Set<EntityType> selectRandomMobs() {
        Map<EntityType, Double> mobChances = configManager.getMobChances();
        int count = configManager.getMobSelectionCount();
        List<EntityType> available = new ArrayList<>(mobChances.keySet());
        Set<EntityType> selected = new HashSet<>();

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            double total = available.stream().mapToDouble(mobChances::get).sum();
            double roll = random.nextDouble() * total;
            double acc = 0;
            for (EntityType mob : available) {
                acc += mobChances.get(mob);
                if (roll <= acc) {
                    selected.add(mob);
                    available.remove(mob);
                    break;
                }
            }
        }
        return selected;
    }
}
