package me.cheat.client.events;

public class RenderEvent extends Event {
    private final float partialTicks;

    public RenderEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() { return partialTicks; }
}
