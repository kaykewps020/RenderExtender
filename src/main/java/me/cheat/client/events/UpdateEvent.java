package me.cheat.client.events;

public class UpdateEvent extends Event {
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean rotating;
    private double x, y, z;

    public UpdateEvent(float yaw, float pitch, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isOnGround() { return onGround; }
    public boolean isRotating() { return rotating; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public void setYaw(float yaw) { this.yaw = yaw; this.rotating = true; }
    public void setPitch(float pitch) { this.pitch = pitch; this.rotating = true; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }
    public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
}
