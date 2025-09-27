package com.ExtraBossRush.GoM.Skill;

import com.ExtraBossRush.GoM.Entity.GoMEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class GoMSkillEvent extends Event {
    private final GoMEntity boss;
    private final ServerPlayer target;

    public GoMSkillEvent(GoMEntity boss, ServerPlayer target) {
        this.boss = boss;
        this.target = target;
    }

    public GoMEntity getBoss() {
        return boss;
    }

    public ServerPlayer getTarget() {
        return target;
    }
}