package com.ExtraBossRush.GoM.Skill;

import com.ExtraBossRush.GoM.Entity.GoMEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class GoMSkillEvent extends Event {
    private final GoMEntity boss;
    private final ServerPlayer target;
    private final int skillId;
    public GoMSkillEvent(GoMEntity boss, ServerPlayer target, int skillId) {
        this.boss = boss;
        this.target = target;
        this.skillId = skillId;
    }
    public GoMEntity getBoss() {
        return boss;
    }
    public ServerPlayer getTarget() {
        return target;
    }
    public int getSkillId() {
        return skillId;
    }
}