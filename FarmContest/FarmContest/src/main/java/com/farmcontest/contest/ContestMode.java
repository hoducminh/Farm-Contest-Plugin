package com.farmcontest.contest;

import java.util.UUID;

public interface ContestMode {
    String id();
    void onStart(ContestContext ctx);
    void onPointSource(ContestContext ctx, UUID player, int points);
    void onEnd(ContestContext ctx);
}
