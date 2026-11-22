package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.api.MineSkyAPI;
import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ReferralConviteDialog {

    private static final String INPUT_REDEEM = "codigo_convite";
    private static final String INPUT_CUSTOM = "codigo_custom";

    private ReferralConviteDialog() {}

    public static void open(
            Player player,
            ReferralManager referral,
            FeedbackManager feedback,
            MessagesManager messages,
            ReferralMenuUi.ReferralMenuSnapshot snapshot) {
        if (player == null || !player.isOnline() || snapshot == null) {
            return;
        }
        boolean bedrock = MineSkyAPI.getDialog() != null && MineSkyAPI.getDialog().isBedrock(player);

        MineSkyDialog.Builder builder = MineSkyDialog.builder(
                        messages.componentOr("referral.menu.title", "&#7BED9FConvites"))
                .multiActionColumns(1);

        builder.body(DialogComponentUtils.parse("&7Seu código"));
        builder.body(Component.text(snapshot.ownCode(), NamedTextColor.GREEN, TextDecoration.BOLD));
        builder.body(DialogComponentUtils.parse(
                "&7Indicações válidas: &f" + snapshot.validReferralCount()));

        if (snapshot.hasInviteeEverRedeemed()) {
            builder.body(DialogComponentUtils.parse(
                    "&7Código usado: &f" + snapshot.usedCodeOrDash()));
            builder.body(DialogComponentUtils.parse(
                    "&7Status: &f" + snapshot.usedStatusLabel()));
        }

        builder.button(DialogButton.builder()
                .label(Component.text("Copiar Código", NamedTextColor.AQUA))
                .width(220)
                .closeOnClick(false)
                .onClick(p -> copyCode(p, feedback, messages, snapshot.ownCode(), bedrock))
                .build());

        if (ReferralMenuUi.showRedeemAction(snapshot.hasInviteeEverRedeemed())) {
            builder.button(DialogButton.builder()
                    .label(Component.text("Usar Código de Amigo", NamedTextColor.YELLOW))
                    .width(220)
                    .closeOnClick(false)
                    .onClick(p -> openRedeemDialog(p, referral, feedback, messages, snapshot))
                    .build());
        }

        if (ReferralMenuUi.showCustomCodeAction(snapshot.customCodeEnabled(), snapshot.customCodePermission())) {
            builder.button(DialogButton.builder()
                    .label(Component.text("Customizar Código", NamedTextColor.LIGHT_PURPLE))
                    .width(220)
                    .closeOnClick(false)
                    .onClick(p -> openCustomCodeDialog(p, referral, feedback, messages, snapshot))
                    .build());
        }

        builder.show(player);
    }

    private static void copyCode(
            Player player,
            FeedbackManager feedback,
            MessagesManager messages,
            String code,
            boolean bedrock) {
        if (code == null || code.isBlank()) {
            return;
        }
        if (bedrock) {
            player.sendMessage(messages.componentOr(
                    "referral.menu.copy.bedrock",
                    "&#7BED9FSeu código: &f&l{code}",
                    "code",
                    code));
        } else {
            Component clickable = Component.text(code, NamedTextColor.GREEN, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.copyToClipboard(code))
                    .hoverEvent(HoverEvent.showText(Component.text("Clique para copiar", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("Código (clique para copiar): ", NamedTextColor.GRAY).append(clickable));
        }
        feedback.send(
                player,
                messages.componentOr("referral.menu.copy.ok", "&#7BED9FCódigo enviado — clique no chat para copiar."),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
    }

    private static void openRedeemDialog(
            Player player,
            ReferralManager referral,
            FeedbackManager feedback,
            MessagesManager messages,
            ReferralMenuUi.ReferralMenuSnapshot snapshot) {
        AtomicBoolean handled = new AtomicBoolean();
        MineSkyDialog.builder(messages.componentOr("referral.menu.redeem.title", "&#FFD166Usar código de amigo"))
                .body(messages.componentOr(
                        "referral.menu.redeem.body",
                        "&7Digite o código que você recebeu."))
                .addInput(TextInput.builder(INPUT_REDEEM, Component.text("Código"))
                        .placeholder("Ex: ABC123")
                        .maxLength(16)
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Confirmar", NamedTextColor.GREEN))
                        .width(180)
                        .closeOnClick(true)
                        .onClick((p, response) -> confirmRedeem(handled, p, response, referral, feedback, messages, snapshot))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Voltar", NamedTextColor.GRAY))
                        .width(150)
                        .closeOnClick(false)
                        .onClick(p -> open(p, referral, feedback, messages, snapshot))
                        .build())
                .show(player);
    }

    private static void confirmRedeem(
            AtomicBoolean handled,
            Player player,
            DialogResponse response,
            ReferralManager referral,
            FeedbackManager feedback,
            MessagesManager messages,
            ReferralMenuUi.ReferralMenuSnapshot snapshot) {
        if (!handled.compareAndSet(false, true)) {
            return;
        }
        String typed = response == null ? "" : response.getText(INPUT_REDEEM, "");
        if (typed == null || typed.isBlank()) {
            feedback.send(
                    player,
                    messages.componentOr("referral.dialog.empty", "&cDigite um código válido."),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            openRedeemDialog(player, referral, feedback, messages, snapshot);
            return;
        }
        referral.redeemCode(player, typed.trim());
        referral.openConviteMenu(player);
    }

    private static void openCustomCodeDialog(
            Player player,
            ReferralManager referral,
            FeedbackManager feedback,
            MessagesManager messages,
            ReferralMenuUi.ReferralMenuSnapshot snapshot) {
        AtomicBoolean handled = new AtomicBoolean();
        MineSkyDialog.builder(messages.componentOr("referral.menu.custom.title", "&#7BED9FCustomizar código"))
                .body(messages.componentOr(
                        "referral.menu.custom.body",
                        "&73 a 16 caracteres: A-Z, 0-9, _ ou -"))
                .addInput(TextInput.builder(INPUT_CUSTOM, Component.text("Novo código"))
                        .placeholder("Ex: MINA_SKY")
                        .maxLength(16)
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Confirmar", NamedTextColor.GREEN))
                        .width(180)
                        .closeOnClick(true)
                        .onClick((p, response) -> confirmCustom(handled, p, response, referral, messages, snapshot))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Voltar", NamedTextColor.GRAY))
                        .width(150)
                        .closeOnClick(false)
                        .onClick(p -> open(p, referral, feedback, messages, snapshot))
                        .build())
                .show(player);
    }

    private static void confirmCustom(
            AtomicBoolean handled,
            Player player,
            DialogResponse response,
            ReferralManager referral,
            MessagesManager messages,
            ReferralMenuUi.ReferralMenuSnapshot snapshot) {
        if (!handled.compareAndSet(false, true)) {
            return;
        }
        String typed = response == null ? "" : response.getText(INPUT_CUSTOM, "");
        if (typed == null || typed.isBlank()) {
            return;
        }
        referral.setCustomCode(player, typed.trim());
        referral.openConviteMenu(player);
    }
}