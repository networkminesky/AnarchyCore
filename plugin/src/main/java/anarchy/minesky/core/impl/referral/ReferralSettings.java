package anarchy.minesky.core.impl.referral;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class ReferralSettings {

    private final Plugin plugin;
    private boolean enabled;
    private String codeCharset;
    private int codeLength;
    private long newAccountMaxAgeMs;
    private long qualificationDeadlineMs;
    private long minActivePlaytimeMs;
    private double minDistanceTraveled;
    private int minBlocksMined;
    private int minBlocksPlaced;
    private long afkTimeoutMs;
    private double referrerRewardShards;
    private double inviteeBonusShards;
    private int maxRedemptionsPerSubnet;
    private boolean checkSameIp;
    private boolean checkSameSubnet;
    private boolean ipv4Slash24;
    private boolean ipv6Slash64;
    private int maxRewardsPerDay;
    private int maxRewardsPerWeek;
    private int maxRewardsTotal;
    private long redeemCooldownMs;
    private int maxCodeGenerationAttempts;
    private int cycleMaxDepth;

    public ReferralSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        if (!config.isSet("referral.enabled")) {
            plugin.getLogger().warning("[Referral] config.yml sem referral.enabled — sistema ligado por padrão.");
        }
        enabled = config.getBoolean("referral.enabled", true);
        if (!enabled) {
            plugin.getLogger().warning("[Referral] Sistema de convites desativado (referral.enabled: false).");
        }
        codeCharset = config.getString("referral.code.charset", ReferralCodeGenerator.DEFAULT_CHARSET);
        codeLength = config.getInt("referral.code.length", 8);
        newAccountMaxAgeMs = config.getLong("referral.security.new-account-max-age-hours", 24L) * 3_600_000L;
        qualificationDeadlineMs = config.getLong("referral.qualification.deadline-days", 7L) * 86_400_000L;
        minActivePlaytimeMs = config.getLong("referral.qualification.min-active-playtime-minutes", 120L) * 60_000L;
        minDistanceTraveled = config.getDouble("referral.qualification.min-distance", 5000.0);
        minBlocksMined = config.getInt("referral.qualification.min-blocks-mined", 100);
        minBlocksPlaced = config.getInt("referral.qualification.min-blocks-placed", 25);
        afkTimeoutMs = config.getLong("referral.qualification.afk-timeout-seconds", 120L) * 1000L;
        referrerRewardShards = config.getDouble("referral.rewards.referrer-shards", 50.0);
        inviteeBonusShards = config.getDouble("referral.rewards.invitee-bonus-shards", 10.0);
        maxRedemptionsPerSubnet = config.getInt("referral.security.max-redemptions-per-subnet", 1);
        checkSameIp = config.getBoolean("referral.security.check-same-ip", true);
        checkSameSubnet = config.getBoolean("referral.security.check-same-subnet", true);
        ipv4Slash24 = config.getBoolean("referral.security.ipv4-slash24", true);
        ipv6Slash64 = config.getBoolean("referral.security.ipv6-slash64", true);
        maxRewardsPerDay = config.getInt("referral.caps.per-day", 5);
        maxRewardsPerWeek = config.getInt("referral.caps.per-week", 20);
        maxRewardsTotal = config.getInt("referral.caps.total", 200);
        redeemCooldownMs = config.getLong("referral.security.redeem-cooldown-seconds", 5L) * 1000L;
        maxCodeGenerationAttempts = config.getInt("referral.code.max-generation-attempts", 20);
        cycleMaxDepth = Math.max(1, config.getInt("referral.security.cycle-depth", 16));
    }

    public boolean enabled() {
        return enabled;
    }

    public String codeCharset() {
        return codeCharset;
    }

    public int codeLength() {
        return codeLength;
    }

    public long newAccountMaxAgeMs() {
        return newAccountMaxAgeMs;
    }

    public long qualificationDeadlineMs() {
        return qualificationDeadlineMs;
    }

    public long minActivePlaytimeMs() {
        return minActivePlaytimeMs;
    }

    public double minDistanceTraveled() {
        return minDistanceTraveled;
    }

    public int minBlocksMined() {
        return minBlocksMined;
    }

    public int minBlocksPlaced() {
        return minBlocksPlaced;
    }

    public long afkTimeoutMs() {
        return afkTimeoutMs;
    }

    public double referrerRewardShards() {
        return referrerRewardShards;
    }

    public double inviteeBonusShards() {
        return inviteeBonusShards;
    }

    public int maxRedemptionsPerSubnet() {
        return maxRedemptionsPerSubnet;
    }

    public boolean checkSameIp() {
        return checkSameIp;
    }

    public boolean checkSameSubnet() {
        return checkSameSubnet;
    }

    public boolean ipv4Slash24() {
        return ipv4Slash24;
    }

    public boolean ipv6Slash64() {
        return ipv6Slash64;
    }

    public int maxRewardsPerDay() {
        return maxRewardsPerDay;
    }

    public int maxRewardsPerWeek() {
        return maxRewardsPerWeek;
    }

    public int maxRewardsTotal() {
        return maxRewardsTotal;
    }

    public long redeemCooldownMs() {
        return redeemCooldownMs;
    }

    public int maxCodeGenerationAttempts() {
        return maxCodeGenerationAttempts;
    }

    public int cycleMaxDepth() {
        return cycleMaxDepth;
    }
}