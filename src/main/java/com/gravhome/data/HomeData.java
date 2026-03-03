package com.gravhome.data;

import net.minecraft.nbt.NbtCompound;

public class HomeData {
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String dimension;

    public HomeData(double x, double y, double z, float yaw, float pitch, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }

    public static HomeData fromNbt(NbtCompound nbt) {
        return new HomeData(
                nbt.getDouble("x"),
                nbt.getDouble("y"),
                nbt.getDouble("z"),
                nbt.getFloat("yaw"),
                nbt.getFloat("pitch"),
                nbt.getString("dimension"));
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble("x", x);
        nbt.putDouble("y", y);
        nbt.putDouble("z", z);
        nbt.putFloat("yaw", yaw);
        nbt.putFloat("pitch", pitch);
        nbt.putString("dimension", dimension);
        return nbt;
    }

    // Getters
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getDimension() {
        return dimension;
    }
}
