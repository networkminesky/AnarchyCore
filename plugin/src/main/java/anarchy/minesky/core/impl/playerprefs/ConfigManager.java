package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.tpa.TpaSounds;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ConfigManager {

    private static final String KEY_TPA_STRANGERS = "tpa_strangers";
    private static final String KEY_TPAHERE_STRANGERS = "tpahere_strangers";
    private static final String KEY_PAY_STRANGERS = "pay_strangers";
    private static final String KEY_PAYMENT_SOUNDS = "payment_sounds";
    private static final String KEY_TELL_STRANGERS = "tell_strangers";
    private static final String KEY_TELL_SOUNDS = "tell_sounds";
    private static final String KEY_SCOREBOARD = "scoreboard_enabled";

    public enum Category {
        TPA,
        MONEY,
        TELL,
        SCOREBOARD
    }

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PlayerPreferencesService prefs;
    private final ConfigSettings settings;

    public ConfigManager(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            PlayerPreferencesService prefs,
            ConfigSettings settings) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.prefs = prefs;
        this.settings = settings;
    }

    public void openMainMenu(Player player) {
        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.raw("config.dialog.main.title"))
                .bodyItemIcon(new ItemStack(Material.COMPARATOR))
                .multiActionColumns(1);

        builder.button(categoryButton(Category.TPA, player));
        builder.button(categoryButton(Category.MONEY, player));
        builder.button(categoryButton(Category.TELL, player));
        builder.button(categoryButton(Category.SCOREBOARD, player));

        builder.show(player);
    }

    public void openCategory(Player player, Category category) {
        switch (category) {
            case TPA -> openTpaCategory(player);
            case MONEY -> openMoneyCategory(player);
            case TELL -> openTellCategory(player);
            case SCOREBOARD -> openScoreboardCategory(player);
        }
    }

    private void openTpaCategory(Player player) {
        PlayerPreferences p = prefs.get(player);
        MineSkyDialog.Builder builder = categoryShell("config.dialog.categories.tpa.title",
                (pl, res) -> syncTpaPrefs(pl, res));
        builder.boolInput(KEY_TPA_STRANGERS, messages.raw("config.options.tpa-strangers"), p.allowTpaFromStrangers());
        builder.boolInput(KEY_TPAHERE_STRANGERS, messages.raw("config.options.tpahere-strangers"),
                p.allowTpaHereFromStrangers());
        addApply(builder);
        addBack(builder, player);
        builder.show(player);
    }

    private void openMoneyCategory(Player player) {
        PlayerPreferences p = prefs.get(player);
        MineSkyDialog.Builder builder = categoryShell("config.dialog.categories.money.title",
                (pl, res) -> syncMoneyPrefs(pl, res));
        builder.boolInput(KEY_PAY_STRANGERS, messages.raw("config.options.pay-strangers"), p.allowPayFromStrangers());
        builder.boolInput(KEY_PAYMENT_SOUNDS, messages.raw("config.options.payment-sounds"), p.paymentSounds());
        addApply(builder);
        addBack(builder, player);
        builder.show(player);
    }

    private void openTellCategory(Player player) {
        PlayerPreferences p = prefs.get(player);
        MineSkyDialog.Builder builder = categoryShell("config.dialog.categories.tell.title",
                (pl, res) -> syncTellPrefs(pl, res));
        builder.boolInput(KEY_TELL_STRANGERS, messages.raw("config.options.tell-strangers"), p.allowTellFromStrangers());
        builder.boolInput(KEY_TELL_SOUNDS, messages.raw("config.options.tell-sounds"), p.tellNotificationSounds());
        addApply(builder);
        addBack(builder, player);
        builder.show(player);
    }

    private void openScoreboardCategory(Player player) {
        PlayerPreferences p = prefs.get(player);
        MineSkyDialog.Builder builder = categoryShell("config.dialog.categories.scoreboard.title",
                (pl, res) -> syncScoreboardPrefs(pl, res));
        builder.boolInput(KEY_SCOREBOARD, messages.raw("config.options.scoreboard-enabled"), p.scoreboardEnabled());
        addApply(builder);
        addBack(builder, player);
        builder.show(player);
    }

    private MineSkyDialog.Builder categoryShell(String titlePath, BiConsumer<Player, DialogResponse> sync) {
        return MineSkyDialog.builder(messages.raw(titlePath))
                .multiActionColumns(1)
                .onSubmit(sync);
    }

    private void syncTpaPrefs(Player player, DialogResponse res) {
        syncBool(player, res, KEY_TPA_STRANGERS, "config.options.tpa-strangers",
                prefs.get(player).allowTpaFromStrangers(),
                v -> prefs.update(player, p -> p.setAllowTpaFromStrangers(v)));
        syncBool(player, res, KEY_TPAHERE_STRANGERS, "config.options.tpahere-strangers",
                prefs.get(player).allowTpaHereFromStrangers(),
                v -> prefs.update(player, p -> p.setAllowTpaHereFromStrangers(v)));
    }

    private void syncMoneyPrefs(Player player, DialogResponse res) {
        syncBool(player, res, KEY_PAY_STRANGERS, "config.options.pay-strangers",
                prefs.get(player).allowPayFromStrangers(),
                v -> prefs.update(player, p -> p.setAllowPayFromStrangers(v)));
        syncBool(player, res, KEY_PAYMENT_SOUNDS, "config.options.payment-sounds",
                prefs.get(player).paymentSounds(),
                v -> prefs.update(player, p -> p.setPaymentSounds(v)));
    }

    private void syncTellPrefs(Player player, DialogResponse res) {
        syncBool(player, res, KEY_TELL_STRANGERS, "config.options.tell-strangers",
                prefs.get(player).allowTellFromStrangers(),
                v -> prefs.update(player, p -> p.setAllowTellFromStrangers(v)));
        syncBool(player, res, KEY_TELL_SOUNDS, "config.options.tell-sounds",
                prefs.get(player).tellNotificationSounds(),
                v -> prefs.update(player, p -> p.setTellNotificationSounds(v)));
    }

    private void syncScoreboardPrefs(Player player, DialogResponse res) {
        syncBool(player, res, KEY_SCOREBOARD, "config.options.scoreboard-enabled",
                prefs.get(player).scoreboardEnabled(),
                v -> prefs.update(player, p -> p.setScoreboardEnabled(v)));
    }

    private void syncBool(
            Player player,
            DialogResponse res,
            String key,
            String optionLabelPath,
            boolean current,
            Consumer<Boolean> apply) {
        boolean next = res.getBoolean(key, current);
        if (next == current) {
            return;
        }
        apply.accept(next);
        playToggleSound(player, next);
        notifyToggle(player, optionLabelPath, next);
    }

    private DialogButton categoryButton(Category category, Player player) {
        String labelPath = switch (category) {
            case TPA -> "config.dialog.categories.tpa.button";
            case MONEY -> "config.dialog.categories.money.button";
            case TELL -> "config.dialog.categories.tell.button";
            case SCOREBOARD -> "config.dialog.categories.scoreboard.button";
        };
        return DialogButton.builder()
                .label(messages.raw(labelPath))
                .width(260)
                .onClick(p -> openCategory(p, category))
                .build();
    }

    private void addApply(MineSkyDialog.Builder builder) {
        builder.button(DialogButton.builder()
                .label(messages.raw("config.dialog.apply"))
                .width(200)
                .closeOnClick(false)
                .onClick((player, response) -> { })
                .build());
    }

    private void addBack(MineSkyDialog.Builder builder, Player player) {
        builder.button(DialogButton.builder()
                .label(messages.raw("config.dialog.back"))
                .width(200)
                .onClick(this::openMainMenu)
                .build());
    }

    private void playToggleSound(Player player, boolean enabled) {
        TpaSounds.play(player,
                enabled ? settings.toggleOn() : settings.toggleOff(),
                settings.volume(),
                settings.pitch());
    }

    private void notifyToggle(Player player, String optionLabelPath, boolean enabled) {
        String messagePath = enabled ? "config.toggle.enabled" : "config.toggle.disabled";
        feedback.send(player, messages.component(messagePath, "option", messages.raw(optionLabelPath)),
                FeedbackPriority.NORMAL);
    }

    public void playTellNotification(Player target) {
        if (!prefs.get(target).tellNotificationSounds()) {
            return;
        }
        TpaSounds.play(target, settings.tellNotify(), settings.volume(), settings.pitch() + 0.15f);
    }
}
