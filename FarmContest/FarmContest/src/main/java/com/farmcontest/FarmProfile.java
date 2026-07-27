package com.farmcontest;

import java.util.Map;

public class FarmProfile {

    private final int totalContests;
    private final int top1Wins;
    private final int top2Wins;
    private final int top3Wins;
    private final long totalHarvests;
    private final long totalMobKills;
    private final Map<String, Integer> medals;

    public FarmProfile(int totalContests, int top1Wins, int top2Wins, int top3Wins, long totalHarvests,
                        long totalMobKills, Map<String, Integer> medals) {
        this.totalContests = totalContests;
        this.top1Wins = top1Wins;
        this.top2Wins = top2Wins;
        this.top3Wins = top3Wins;
        this.totalHarvests = totalHarvests;
        this.totalMobKills = totalMobKills;
        this.medals = medals;
    }

    public int getTotalContests() { return totalContests; }
    public int getTop1Wins()      { return top1Wins; }
    public int getTop2Wins()      { return top2Wins; }
    public int getTop3Wins()      { return top3Wins; }
    public long getTotalHarvests() { return totalHarvests; }
    public long getTotalMobKills() { return totalMobKills; }
    public int getMedalCount(String tier) { return medals.getOrDefault(tier, 0); }
    public Map<String, Integer> getMedals() { return medals; }
}
