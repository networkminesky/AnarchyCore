package anarchy.minesky.core.impl.referral;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    private boolean customCodeEnabled;
    private String customCodePermission;
    private ReferralCustomCodeRules customCodeRules;
    private boolean welcomeDialogEnabled;
    private long welcomeDialogDelayTicks;
    private String welcomeTitle;
    private String welcomeBody;
    private String welcomeInputLabel;
    private String welcomeInputPlaceholder;
    private String welcomeConfirm;
    private String welcomeSkip;

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
        customCodeEnabled = config.getBoolean("referral.custom-code.enabled", true);
        String permission = config.getString("referral.custom-code.permission", "anarchycore.referral.customcode");
        customCodePermission = permission == null || permission.isBlank()
                ? "anarchycore.referral.customcode"
                : permission.trim();
        customCodeRules = loadCustomCodeRules(config);
        welcomeDialogEnabled = config.getBoolean("referral.first-join-dialog.enabled", true);
        int delay = config.getInt("referral.first-join-dialog.delay-ticks", 30);
        if (delay < 1) {
            delay = 1;
        } else if (delay > 1200) {
            delay = 1200;
        }
        welcomeDialogDelayTicks = delay;
        welcomeTitle = text(config, "referral.first-join-dialog.title", "&#7BED9FConvite");
        welcomeBody = text(config, "referral.first-join-dialog.body",
                "&7Se alguém te convidou, digite o código abaixo. Você também pode usar &f/convite usar <codigo>&7 dentro do prazo.");
        welcomeInputLabel = text(config, "referral.first-join-dialog.input-label", "&fCódigo de convite");
        welcomeInputPlaceholder = text(config, "referral.first-join-dialog.input-placeholder", "CODIGO");
        welcomeConfirm = text(config, "referral.first-join-dialog.confirm-button", "&#7BED9FResgatar");
        welcomeSkip = text(config, "referral.first-join-dialog.skip-button", "&7Não tenho código");
    }

    private ReferralCustomCodeRules loadCustomCodeRules(FileConfiguration config) {
        int max = config.getInt("referral.custom-code.max-length", 16);
        if (max > 16) {
            plugin.getLogger().warning("[Referral] custom-code.max-length acima de 16 foi limitado a 16.");
            max = 16;
        }
        if (max < 1) {
            max = 1;
        }
        int min = config.getInt("referral.custom-code.min-length", 3);
        if (min < 1) {
            min = 1;
        }
        if (min > max) {
            min = max;
        }
        String pattern = config.getString("referral.custom-code.pattern", ReferralCustomCodeRules.DEFAULT_PATTERN);
        if (pattern != null && !pattern.isBlank()) {
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException error) {
                plugin.getLogger().log(Level.WARNING,
                        "[Referral] regex de código custom inválida; usando o padrão.", error);
                pattern = ReferralCustomCodeRules.DEFAULT_PATTERN;
            }
        }
        long cooldownMs = Math.max(0L, config.getLong("referral.custom-code.change-cooldown-seconds", 86_400L)) * 1000L;
        int maxChanges = Math.max(0, config.getInt("referral.custom-code.max-changes", 5));
        List<String> reserved = config.getStringList("referral.custom-code.reserved");
        List<String> blacklist = config.getStringList("referral.custom-code.blacklist");
        return new ReferralCustomCodeRules(pattern, min, max, cooldownMs, maxChanges, reserved, blacklist);
    }

    private static String text(FileConfiguration config, String path, String fallback) {
        String value = config.getString(path, fallback);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
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

    public boolean customCodeEnabled() {
        return customCodeEnabled;
    }

    public String customCodePermission() {
        return customCodePermission;
    }

    public ReferralCustomCodeRules customCodeRules() {
        return customCodeRules;
    }

    public boolean welcomeDialogEnabled() {
        return welcomeDialogEnabled;
    }

    public long welcomeDialogDelayTicks() {
        return welcomeDialogDelayTicks;
    }

    public String welcomeTitle() {
        return welcomeTitle;
    }

    public String welcomeBody() {
        return welcomeBody;
    }

    public String welcomeInputLabel() {
        return welcomeInputLabel;
    }

    public String welcomeInputPlaceholder() {
        return welcomeInputPlaceholder;
    }

    public String welcomeConfirm() {
        return welcomeConfirm;
    }

    public String welcomeSkip() {
        return welcomeSkip;
    }
}