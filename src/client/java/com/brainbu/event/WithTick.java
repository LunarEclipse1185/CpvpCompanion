package com.brainbu.event;

public class WithTick<T> {
    public long tick;
    public T content;

    WithTick(long tick, T content) {
        this.tick = tick;
        this.content = content;
    }
}
