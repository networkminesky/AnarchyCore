package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * O renderer Java e o formulário Bedrock disparam {@code onSubmit} e o clique do botão
 * no mesmo envio quando há input. O resgate fica só no botão Confirmar, com trava única.
 */
public final class ReferralWelcomeDialog {

    private ReferralWelcomeDialog() {}

    public static void open(Player player, ReferralSettings settings, ReferralManager referral) {
        AtomicBoolean handled = new AtomicBoolean();
        MineSkyDialog.builder(settings.welcomeTitle())
                .body(settings.welcomeBody())
                .canCloseWithEscape(true)
                .textInput(
                        "codigo_convite",
                        settings.welcomeInputLabel(),
                        settings.welcomeInputPlaceholder(),
                        "")
                .button(DialogButton.builder()
                        .label(settings.welcomeConfirm())
                        .closeOnClick(true)
                        .onClick((clicked, response) -> confirm(handled, clicked, response, referral))
                        .build())
                .button(DialogButton.builder()
                        .label(settings.welcomeSkip())
                        .closeOnClick(true)
                        .onClick(clicked -> skip(handled, clicked, referral))
                        .build())
                .show(player);
    }

    private static void confirm(
            AtomicBoolean handled,
            Player player,
            DialogResponse response,
            ReferralManager referral) {
        if (!handled.compareAndSet(false, true)) {
            return;
        }
        referral.markWelcomeDialogSeen(player.getUniqueId());
        String typed = response == null ? "" : response.getText("codigo_convite", "");
        if (typed == null) {
            typed = "";
        }
        typed = typed.trim();
        if (typed.isEmpty()) {
            referral.sendWelcomeEmpty(player);
            return;
        }
        referral.redeemCode(player, typed);
    }

    private static void skip(AtomicBoolean handled, Player player, ReferralManager referral) {
        if (!handled.compareAndSet(false, true)) {
            return;
        }
        referral.markWelcomeDialogSeen(player.getUniqueId());
        referral.sendWelcomeSkipped(player);
    }
}
