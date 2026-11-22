package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.api.MineSkyAPI;
import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.api.utils.HeadURLResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class PlayerProfileDialog {

    private static final int BUTTON_WIDTH = 200;

    private PlayerProfileDialog() {}

    public static void openSummary(
            Player viewer,
            PlayerProfileSnapshot profile,
            Runnable openFullGui,
            Runnable back) {
        if (viewer == null || !viewer.isOnline() || profile == null) {
            return;
        }
        boolean bedrock = MineSkyAPI.getDialog() != null && MineSkyAPI.getDialog().isBedrock(viewer);

        MineSkyDialog.Builder builder = MineSkyDialog.builder(
                        Component.text(profile.displayName(), NamedTextColor.WHITE))
                .multiActionColumns(1);

        if (bedrock) {
            builder.body(Component.text(profile.displayName(), NamedTextColor.GRAY));
        } else {
            Component head = HeadURLResolver.playerHeadFor(profile.uuid().toString());
            if (head != null && !head.equals(Component.empty())) {
                builder.body(head);
            }
        }
        builder.body(DialogComponentUtils.parse(PlayerProfileFormatter.formatMoneyLine(profile.money())));
        builder.body(DialogComponentUtils.parse(PlayerProfileFormatter.formatKillsLine(profile.kills())));
        builder.body(DialogComponentUtils.parse(PlayerProfileFormatter.formatDeathsLine(profile.deaths())));
        builder.body(DialogComponentUtils.parse(PlayerProfileFormatter.formatPlaytimeLine(profile.playtimeSeconds())));

        builder.button(DialogButton.builder()
                .label(Component.text("Ver Perfil Completo", NamedTextColor.WHITE))
                .width(BUTTON_WIDTH)
                .closeOnClick(true)
                .onClick(p -> {
                    if (openFullGui != null) {
                        openFullGui.run();
                    }
                })
                .build());

        if (back != null) {
            builder.button(DialogButton.builder()
                    .label(Component.text("Voltar", NamedTextColor.GRAY))
                    .width(BUTTON_WIDTH)
                    .closeOnClick(false)
                    .onClick(p -> back.run())
                    .build());
        } else {
            builder.button(DialogButton.builder()
                    .label(Component.text("Fechar", NamedTextColor.GRAY))
                    .width(BUTTON_WIDTH)
                    .closeOnClick(true)
                    .onClick(Player::closeDialog)
                    .build());
        }

        builder.show(viewer);
    }
}