package com.ExtraBossRush.GoM.Support.Time;

import net.minecraftforge.eventbus.api.Event;

public class TimeChangeEvent extends Event {
    private final int newTime;

    public TimeChangeEvent(int newTime) {
        this.newTime = newTime;
    }

    public int getNewTime() {
        return newTime;
    }
}