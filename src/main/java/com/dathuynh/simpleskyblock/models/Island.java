package com.dathuynh.simpleskyblock.models;

import org.bukkit.Location;

import java.util.*;

public class Island {
    private UUID owner;
    private Location location;
    private Set<UUID> members;
    private long createdTime;
    private long deletedTime; // 0 nếu chưa xóa

    public Island(UUID owner, Location location) {
        this.owner = owner;
        this.location = location;
        this.members = new HashSet<>();
        this.members.add(owner); // Owner cũng là member
        this.createdTime = System.currentTimeMillis();
        this.deletedTime = 0;
    }

    // Getters/Setters
    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(long time) {
        this.deletedTime = time;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }
}
