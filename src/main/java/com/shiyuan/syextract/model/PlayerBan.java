package com.shiyuan.syextract.model;

import java.util.UUID;

public class PlayerBan {

    private final UUID playerId;
    private final String playerName;
    private final long banTime;
    private final long expireTime;
    private final boolean banCreate;
    private final boolean banClaim;

    public PlayerBan(UUID playerId, String playerName, long durationHours, boolean banCreate, boolean banClaim) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.banTime = System.currentTimeMillis();
        this.expireTime = this.banTime + (durationHours * 60 * 60 * 1000);
        this.banCreate = banCreate;
        this.banClaim = banClaim;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public boolean canCreate() {
        return !banCreate || isExpired();
    }

    public boolean canClaim() {
        return !banClaim || isExpired();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getBanTime() {
        return banTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public boolean isBanCreate() {
        return banCreate;
    }

    public boolean isBanClaim() {
        return banClaim;
    }
}
