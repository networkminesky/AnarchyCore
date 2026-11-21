package anarchy.minesky.core.impl.escmenu;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.escmenu.economy.AnarchyEconomyMoneyProvider;
import anarchy.minesky.core.impl.escmenu.flows.EscHubPayFlow;
import anarchy.minesky.core.impl.escmenu.flows.EscHubStatsFlow;
import anarchy.minesky.core.impl.escmenu.flows.EscHubTpaFlow;
import anarchy.minesky.core.impl.escmenu.flows.OnlinePlayerGridDialog;
import anarchy.minesky.core.impl.profile.PlayerProfileFlow;
import anarchy.minesky.core.impl.friends.FriendsManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.leaderboards.LeaderboardDialog;
import anarchy.minesky.core.impl.playerprefs.ConfigManager;
import anarchy.minesky.core.impl.referral.ReferralManager;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class EscMenuRuntime {

    private final Plugin plugin;
    private final EscMenuDefinition definition;
    private final Map<String, Consumer<Player>> handlers = new HashMap<>();

    public EscMenuRuntime(
            Plugin plugin,
            EscMenuDefinition definition,
            DialogManager dialogManager,
            ConfigManager config,
            ReferralManager referral,
            FriendsManager friends,
            FeedbackManager feedback,
            AnarchyEconomyMoneyProvider economy,
            LeaderboardDialog leaderboardDialog,
            PlayerProfileFlow playerProfiles) {
        this.plugin = plugin;
        this.definition = definition;

        OnlinePlayerGridDialog playerGrid = new OnlinePlayerGridDialog(dialogManager, definition.flows());
        EscHubTpaFlow tpaFlow = new EscHubTpaFlow(plugin, dialogManager, definition, playerGrid, this::showHub);
        EscHubPayFlow payFlow = new EscHubPayFlow(
                plugin,
                definition,
                dialogManager,
                playerGrid,
                economy,
                feedback,
                this::showHub,
                definition.flows().payFallbackCommand());

        handlers.put("config", config::openMainMenu);
        handlers.put("convite", referral::openConviteMenu);
        handlers.put("leaderboards", leaderboardDialog::openMain);
        handlers.put("stats", new EscHubStatsFlow(plugin, playerGrid, playerProfiles, this::showHub)::start);
        handlers.put("friends", friends::openMainDialog);
        handlers.put("tpa", tpaFlow::start);
        handlers.put("pay", payFlow::start);
        handlers.put("_exit", Player::closeDialog);
    }

    public EscMenuDefinition definition() {
        return definition;
    }

    public void showHub(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        MineSkyDialog.Builder builder = MineSkyDialog.builder(definition.hubTitle())
                .multiActionColumns(definition.columns());
        for (EscMenuButton button : definition.buttons()) {
            if (!hasPermission(player, button.permission())) {
                continue;
            }
            builder.button(toDialogButton(button));
        }
        builder.show(player);
    }

    public void handleCustomClick(Key key, Player player) {
        if (!EscMenuKeys.isEscCustomClick(key)) {
            return;
        }
        String buttonId = EscMenuKeys.buttonIdFromKey(key);
        if (buttonId == null) {
            return;
        }
        EscMenuButton button = definition.findButton(buttonId);
        if (button != null && !hasPermission(player, button.permission())) {
            return;
        }
        if ("_exit".equals(buttonId)) {
            player.closeDialog();
            return;
        }
        if (button == null || button.type() != EscMenuButtonType.CUSTOM) {
            return;
        }
        Consumer<Player> handler = handlers.get(button.handler().toLowerCase(Locale.ROOT));
        if (handler == null) {
            plugin.getLogger().warning("[AnarchyCore] Menu ESC: handler desconhecido '" + button.handler()
                    + "' (botão " + buttonId + ")");
            return;
        }
        handler.accept(player);
    }

    private DialogButton toDialogButton(EscMenuButton button) {
        return DialogButton.builder()
                .label(button.label())
                .width(button.width())
                .onClick(player -> runButtonAction(player, button))
                .build();
    }

    private void runButtonAction(Player player, EscMenuButton button) {
        if (!hasPermission(player, button.permission())) {
            return;
        }
        switch (button.type()) {
            case COMMAND -> {
                String raw = button.command();
                if (raw == null || raw.isBlank()) {
                    return;
                }
                String cmd = raw.startsWith("/") ? raw.substring(1) : raw;
                player.performCommand(cmd);
            }
            case CUSTOM -> handleCustomClick(EscMenuKeys.customClick(button.id()), player);
            default -> { }
        }
    }

    private static boolean hasPermission(Player player, String permission) {
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }
}