package com.farmcontest.contest;

import com.farmcontest.ConfigManager;

import java.util.List;
import java.util.UUID;

public class CommunityMode implements ContestMode {

    private final ContestMode delegate;
    private final ConfigManager configManager;
    private final RewardDistributor rewardDistributor;

    public CommunityMode(ContestMode delegate, ConfigManager configManager, RewardDistributor rewardDistributor) {
        this.delegate = delegate;
        this.configManager = configManager;
        this.rewardDistributor = rewardDistributor;
    }

    @Override
    public String id() {
        return delegate.id() + "-community";
    }

    @Override
    public void onStart(ContestContext ctx) {
        delegate.onStart(ctx);
        ctx.community = true;
        ctx.communityProgress.set(0);
        ctx.reachedMilestones.clear();
    }

    @Override
    public void onPointSource(ContestContext ctx, UUID player, int points) {
        delegate.onPointSource(ctx, player, points);
        long total = ctx.communityProgress.addAndGet(points);
        checkMilestones(ctx, total);
    }

    @Override
    public void onEnd(ContestContext ctx) {
        delegate.onEnd(ctx);
    }

    private void checkMilestones(ContestContext ctx, long total) {
        List<CommunityMilestone> milestones = configManager.getCommunityMilestones();
        for (int i = 0; i < milestones.size(); i++) {
            CommunityMilestone milestone = milestones.get(i);
            if (ctx.reachedMilestones.contains(i)) continue;
            if (total < milestone.target()) continue;

            ctx.reachedMilestones.add(i);
            rewardDistributor.broadcastCommunityRewards(milestone.rewardCommands());
        }
    }

    public long getProgress(ContestContext ctx) {
        return ctx.communityProgress.get();
    }

    public long getNextMilestoneTarget(ContestContext ctx) {
        List<CommunityMilestone> milestones = configManager.getCommunityMilestones();
        for (int i = 0; i < milestones.size(); i++) {
            if (!ctx.reachedMilestones.contains(i)) return milestones.get(i).target();
        }
        return milestones.isEmpty() ? 0 : milestones.get(milestones.size() - 1).target();
    }
}
