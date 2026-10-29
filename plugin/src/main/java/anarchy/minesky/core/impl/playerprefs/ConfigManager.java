package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.inputs.BoolInput;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOption;
import anarchy.minesky.core.api.playerconfig.PlayerConfigCategory;
import anarchy.minesky.core.api.playerconfig.PlayerConfigRegistry;
import anarchy.minesky.core.api.sounds.UiSoundsAPI;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ConfigManager {

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PlayerPreferencesService prefs;
    private final PlayerConfigRegistry registry;
    private final UiSoundsAPI uiSounds;

    public ConfigManager(
            MessagesManager messages,
            FeedbackManager feedback,
            PlayerPreferencesService prefs,
            PlayerConfigRegistry registry,
            UiSoundsAPI uiSounds) {
        this.messages = messages;
        this.feedback = feedback;
        this.prefs = prefs;
        this.registry = registry;
        this.uiSounds = uiSounds;
    }

    public void openMainMenu(Player player) {
        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.raw("config.dialog.main.title"))
                .bodyItemIcon(new ItemStack(Material.COMPARATOR))
                .multiActionColumns(1);

        for (PlayerConfigCategory category : registry.categories()) {
            builder.button(categoryButton(category));
        }

        builder.show(player);
    }

    public void openCategory(Player player, PlayerConfigCategory category) {
        MineSkyDialog.Builder builder = MineSkyDialog.builder(category.title())
                .multiActionColumns(1)
                .onSubmit((pl, res) -> syncCategory(pl, category, res));

        for (PlayerConfigBooleanOption option : category.booleanOptions()) {
            boolean current = option.get(player);
            builder.addInput(BoolInput.of(dialogKey(option), option.label(), current));
        }

        addApply(builder);
        addBack(builder);
        builder.show(player);
    }

    private void syncCategory(Player player, PlayerConfigCategory category, DialogResponse res) {
        for (PlayerConfigBooleanOption option : category.booleanOptions()) {
            boolean current = option.get(player);
            boolean next = res.getBoolean(dialogKey(option), current);
            if (next == current) {
                continue;
            }
            prefs.setConfigBoolean(player, option, next);
            notifyToggle(player, option.label(), next);
        }
    }

    private DialogButton categoryButton(PlayerConfigCategory category) {
        Component label = DialogComponentUtils.materialIconComponent(category.iconMaterial())
                .append(Component.space())
                .append(category.buttonLabel());
        return DialogButton.builder()
                .label(label)
                .width(260)
                .onClick(player -> openCategory(player, category))
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

    private void addBack(MineSkyDialog.Builder builder) {
        builder.button(DialogButton.builder()
                .label(messages.raw("config.dialog.back"))
                .width(200)
                .onClick(this::openMainMenu)
                .build());
    }

    private void notifyToggle(Player player, Component optionLabel, boolean enabled) {
        String messagePath = enabled ? "config.toggle.enabled" : "config.toggle.disabled";
        String optionPlain = LegacyComponentSerializer.legacySection().serialize(optionLabel);
        feedback.send(
                player,
                messages.component(messagePath, "option", optionPlain),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
    }

    public void playTellNotification(Player target) {
        if (!uiSounds.isTellEnabled()) {
            return;
        }
        if (!prefs.get(target).tellNotificationSounds()) {
            return;
        }
        uiSounds.playTell(target);
    }

    /** Chave estável no diálogo (sem {@code :}). */
    private static String dialogKey(PlayerConfigBooleanOption option) {
        return option.key().replace(':', '_');
    }
}
