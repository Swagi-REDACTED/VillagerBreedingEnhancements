package net.blupillcosby.villagerbreedingenhancements.access;

import net.minecraft.server.level.ServerLevel;

public interface InLoveVillager {
    int getInLoveTicks();
    void setInLoveTicks(int ticks);
    void setInLove(ServerLevel level);
}
