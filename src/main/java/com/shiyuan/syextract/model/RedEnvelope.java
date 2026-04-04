package com.shiyuan.syextract.model;

import java.util.*;

public class RedEnvelope {

    private static int nextId = 1;

    private final UUID id;
    private final int numericId;
    private final String shortId;
    private final UUID sender;
    private final String senderName;
    private final String name;
    private final double totalAmount;
    private final int totalCount;
    private final long createTime;
    private final long expireTime;
    private final List<Double> amounts;
    private final Set<UUID> claimedPlayers;
    private final Map<UUID, Double> claimedAmounts;
    private int remainingCount;
    private boolean refunded;

    public RedEnvelope(UUID sender, String senderName, String name, double totalAmount, int totalCount, long expireHours) {
        this.id = UUID.randomUUID();
        this.numericId = nextId++;
        this.shortId = String.valueOf(this.numericId);
        this.sender = sender;
        this.senderName = senderName;
        this.name = name;
        this.totalAmount = totalAmount;
        this.totalCount = totalCount;
        this.createTime = System.currentTimeMillis();
        this.expireTime = this.createTime + (expireHours * 60 * 60 * 1000);
        this.remainingCount = totalCount;
        this.claimedPlayers = new HashSet<>();
        this.claimedAmounts = new HashMap<>();
        this.amounts = generateRandomAmounts(totalAmount, totalCount);
        this.refunded = false;
    }

    public static void setNextId(int id) {
        nextId = id;
    }

    public static int getNextId() {
        return nextId;
    }

    public int getNumericId() {
        return numericId;
    }

    private List<Double> generateRandomAmounts(double total, int count) {
        List<Double> result = new ArrayList<>();
        Random random = new Random();
        
        double remaining = total;
        for (int i = 0; i < count - 1; i++) {
            double max = remaining * 2 / (count - i);
            double amount = random.nextDouble() * max;
            amount = Math.round(amount * 100.0) / 100.0;
            if (amount < 0.01) amount = 0.01;
            result.add(amount);
            remaining -= amount;
        }
        remaining = Math.round(remaining * 100.0) / 100.0;
        result.add(remaining);
        
        Collections.shuffle(result);
        return result;
    }

    public double claim(UUID playerId) {
        if (isExpired() || remainingCount <= 0 || claimedPlayers.contains(playerId)) {
            return -1;
        }

        claimedPlayers.add(playerId);
        remainingCount--;

        int index = totalCount - remainingCount - 1;
        double amount = amounts.get(index);
        claimedAmounts.put(playerId, amount);
        return amount;
    }

    public boolean hasClaimed(UUID playerId) {
        return claimedPlayers.contains(playerId);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public boolean isFullyClaimed() {
        return remainingCount <= 0;
    }

    public double getRemainingAmount() {
        double claimed = 0;
        int claimedCount = totalCount - remainingCount;
        for (int i = 0; i < claimedCount; i++) {
            claimed += amounts.get(i);
        }
        return Math.round((totalAmount - claimed) * 100.0) / 100.0;
    }

    public double getUnclaimedAmount() {
        return getRemainingAmount();
    }

    public boolean isRefunded() {
        return refunded;
    }

    public void setRefunded(boolean refunded) {
        this.refunded = refunded;
    }

    public UUID getId() {
        return id;
    }

    public String getShortId() {
        return shortId;
    }

    public UUID getSender() {
        return sender;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getName() {
        return name;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public Set<UUID> getClaimedPlayers() {
        return new HashSet<>(claimedPlayers);
    }

    public Map<UUID, Double> getClaimedAmounts() {
        return new HashMap<>(claimedAmounts);
    }

    public LuckiestPlayerInfo getLuckiestPlayer() {
        if (claimedAmounts.isEmpty()) {
            return null;
        }

        UUID luckiestPlayerId = null;
        double maxAmount = 0;

        for (Map.Entry<UUID, Double> entry : claimedAmounts.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                luckiestPlayerId = entry.getKey();
            }
        }

        if (luckiestPlayerId == null) {
            return null;
        }

        return new LuckiestPlayerInfo(luckiestPlayerId, maxAmount);
    }

    public static class LuckiestPlayerInfo {
        private final UUID playerId;
        private final double amount;

        public LuckiestPlayerInfo(UUID playerId, double amount) {
            this.playerId = playerId;
            this.amount = amount;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public double getAmount() {
            return amount;
        }
    }
}
