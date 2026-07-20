package me.cheat.client.events;

public class KeyEvent extends Event {
    private final int key;
    private final boolean pressed;

    public KeyEvent(int key, boolean pressed) {
        this.key = key;
        this.pressed = pressed;
    }

    public int getKey() { return key; }
    public boolean isPressed() { return pressed; }
}
