package com.farmcontest;

public class FarmProfile {

    private final int totalContests;
    private final int top1Wins;
    private final int top2Wins;
    private final int top3Wins;
    private final long totalHarvests;

    public FarmProfile(int totalContests, int top1Wins, int top2Wins, int top3Wins, long totalHarvests) {
        this.totalContests = totalContests;
        this.top1Wins = top1Wins;
        this.top2Wins = top2Wins;
        this.top3Wins = top3Wins;
        this.totalHarvests = totalHarvests;
    }

    public int getTotalContests() { return totalContests; }
    public int getTop1Wins()      { return top1Wins; }
    public int getTop2Wins()      { return top2Wins; }
    public int getTop3Wins()      { return top3Wins; }
    public long getTotalHarvests() { return totalHarvests; }
}
