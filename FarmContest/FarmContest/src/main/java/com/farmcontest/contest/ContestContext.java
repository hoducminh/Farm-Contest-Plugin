package com.farmcontest.contest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

public class ContestContext {
    public Set<Material> selectedCrops = new HashSet<>();
    public Set<EntityType> selectedMobs = new HashSet<>();
    public Material luckyCrop;
    public EntityType luckyMob;
    public LocalDateTime contestStartTime;
    public LocalDateTime contestEndTime;
    public boolean community;
    public final AtomicLong communityProgress = new AtomicLong(0);
    public final Set<Integer> reachedMilestones = ConcurrentHashMap.newKeySet();

    public void reset() {
        selectedCrops = new HashSet<>();
        selectedMobs = new HashSet<>();
        luckyCrop = null;
        luckyMob = null;
        community = false;
        communityProgress.set(0);
        reachedMilestones.clear();
    }
}
