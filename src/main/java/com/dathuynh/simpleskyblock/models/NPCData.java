package com.dathuynh.simpleskyblock.models;

import org.bukkit.entity.Villager;

import java.util.List;

public class NPCData {

    private String id;
    private String displayName;
    private Villager.Profession profession;
    private int level;
    private List<TradeData> trades;

    public NPCData(String id, String displayName, Villager.Profession profession, int level, List<TradeData> trades) {
        this.id = id;
        this.displayName = displayName;
        this.profession = profession;
        this.level = level;
        this.trades = trades;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Villager.Profession getProfession() {
        return profession;
    }

    public int getLevel() {
        return level;
    }

    public List<TradeData> getTrades() {
        return trades;
    }
}
