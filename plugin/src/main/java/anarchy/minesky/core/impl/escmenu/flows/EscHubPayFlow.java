package anarchy.minesky.core.impl.escmenu.flows;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.escmenu.EscMenuDefinition;
import anarchy.minesky.core.impl.escmenu.EscMenuFlows;
import anarchy.minesky.core.impl.escmenu.economy.AnarchyEconomyMoneyProvider;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class EscHubPayFlow {

    private final Plugin plugin;
    private final EscMenuDefinition definition;
    private final DialogManager dialog;
    private final OnlinePlayerGridDialog playerGrid;
    private final AnarchyEconomyMoneyProvider economy;
    private final FeedbackManager feedback;
    private final Consumer<Player> openHub;
    private final String fallbackPayCommand;

    public EscHubPayFlow(
            Plugin plugin,
            EscMenuDefinition definition,
            DialogManager dialog,
            OnlinePlayerGridDialog playerGrid,
            AnarchyEconomyMoneyProvider economy,
            FeedbackManager feedback,
            Consumer<Player> openHub,
            String fallbackPayCommand) {
        this.plugin = plugin;
        this.definition = definition;
        this.dialog = dialog;
        this.playerGrid = playerGrid;
        this.economy = economy;
        this.feedback = feedback;
        this.openHub = openHub;
        this.fallbackPayCommand = fallbackPayCommand != null && !fallbackPayCommand.isBlank()
                ? fallbackPayCommand
                : "pay";
    }

    public void start(Player viewer) {
        playerGrid.open(
                viewer,
                Component.text("PIX — escolha um jogador", NamedTextColor.GREEN),
                target -> openAmountMenu(viewer, target),
                openHub);
    }

    private void openAmountMenu(Player viewer, Player target) {
        if (!VisibleOnlinePlayers.isValidTarget(viewer, target)) {
            viewer.sendMessage(DialogComponentUtils.parse("&cJogador não está mais disponível."));
            start(viewer);
            return;
        }
        EscMenuFlows flows = definition.flows();
        int width = flows.selectorButtonWidth();
        boolean bedrock = dialog.isBedrock(viewer);
        Component title = Component.text("PIX para ", NamedTextColor.WHITE)
                .append(Component.text(target.getName(), NamedTextColor.GREEN));

        Component headLine = OnlinePlayerGridDialog.targetBody(bedrock, target);
        MineSkyDialog.Builder builder = MineSkyDialog.builder(title)
                .body(headLine)
                .body(Component.text("Escolha um valor para enviar", NamedTextColor.GRAY))
                .multiActionColumns(2);

        Player capturedTarget = target;
        for (EscMenuFlows.PayPreset preset : flows.payPresets()) {
            double amount = preset.amount();
            builder.button(DialogButton.builder()
                    .label(preset.label())
                    .width(width)
                    .onClick(p -> executePay(p, capturedTarget, amount))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(Component.text("Outro valor", NamedTextColor.WHITE))
                .width(width)
                .closeOnClick(false)
                .onClick(p -> openCustomAmount(p, capturedTarget))
                .build());

        builder.button(DialogButton.builder()
                .label(Component.text("Voltar", NamedTextColor.GRAY))
                .width(width)
                .closeOnClick(false)
                .onClick(p -> start(p))
                .build());

        builder.show(viewer);
    }

    private void openCustomAmount(Player viewer, Player target) {
        EscMenuFlows flows = definition.flows();
        MineSkyDialog.builder(Component.text("Valor personalizado", NamedTextColor.GREEN))
                .body(OnlinePlayerGridDialog.targetBody(dialog.isBedrock(viewer), target))
                .addInput(TextInput.builder(flows.payInputKey(), Component.text("Valor"))
                        .placeholder("Ex: 50 ou 1.000,00")
                        .maxLength(24)
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Confirmar", NamedTextColor.GREEN))
                        .width(180)
                        .closeOnClick(false)
                        .onClick((p, res) -> onCustomAmount(p, target, res))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Voltar", NamedTextColor.GRAY))
                        .width(150)
                        .closeOnClick(false)
                        .onClick(p -> openAmountMenu(p, target))
                        .build())
                .show(viewer);
    }

    private void onCustomAmount(Player viewer, Player target, DialogResponse response) {
        EscMenuFlows flows = definition.flows();
        PixAmountValidator.Result result = PixAmountValidator.validate(
                response.getText(flows.payInputKey(), ""),
                flows.payMin(),
                flows.payMax());
        if (!result.valid()) {
            feedback.send(
                    viewer,
                    payErrorMessage(result.errorKey(), flows),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        executePay(viewer, target, result.amount());
    }

    private void executePay(Player viewer, Player target, double amount) {
        PixAmountValidator.Result validated = PixAmountValidator.validate(amount, definition.flows().payMin(), definition.flows().payMax());
        if (!validated.valid()) {
            feedback.send(viewer, payErrorMessage(validated.errorKey(), definition.flows()),
                    FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return;
        }
        if (!VisibleOnlinePlayers.isValidTarget(viewer, target)) {
            viewer.sendMessage(DialogComponentUtils.parse("&cJogador não está mais disponível."));
            return;
        }
        viewer.closeDialog();

        Player receiver = viewer.getServer().getPlayer(target.getUniqueId());
        if (!VisibleOnlinePlayers.isValidTarget(viewer, receiver)) {
            viewer.sendMessage(DialogComponentUtils.parse("&cJogador não está mais disponível."));
            return;
        }

        if (economy.isAvailable()) {
            economy.pay(viewer, receiver, validated.amount()).thenAccept(outcome -> viewer.getScheduler().run(plugin, task -> {
                if (!viewer.isOnline()) {
                    return;
                }
                handlePayOutcome(viewer, receiver, outcome, validated.amount());
            }, null));
            return;
        }

        viewer.getScheduler().run(plugin, task -> {
            if (!viewer.isOnline()) {
                return;
            }
            String cmd = fallbackPayCommand.startsWith("/")
                    ? fallbackPayCommand.substring(1)
                    : fallbackPayCommand;
            viewer.performCommand(cmd + " " + receiver.getName() + " " + formatCommandAmount(validated.amount()));
        }, null);
    }

    private void handlePayOutcome(
            Player viewer,
            Player receiver,
            AnarchyEconomyMoneyProvider.PayOutcome outcome,
            double amount) {
        if (outcome.status() == AnarchyEconomyMoneyProvider.PayOutcomeStatus.SUCCESS) {
            feedback.send(
                    viewer,
                    DialogComponentUtils.parse("&aPIX enviado: &f$" + formatDisplayAmount(amount)
                            + " &a→ &f" + receiver.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            return;
        }
        if (outcome.status() == AnarchyEconomyMoneyProvider.PayOutcomeStatus.UNAVAILABLE) {
            String cmd = fallbackPayCommand;
            viewer.performCommand((cmd.startsWith("/") ? cmd.substring(1) : cmd)
                    + " " + receiver.getName() + " " + formatCommandAmount(amount));
            return;
        }
        String reason = outcome.reason() != null ? outcome.reason() : "failed";
        Component msg = switch (reason) {
            case "insufficient" -> DialogComponentUtils.parse("&cSaldo insuficiente para este PIX.");
            default -> DialogComponentUtils.parse("&cNão foi possível enviar o PIX. Tente novamente.");
        };
        feedback.send(viewer, msg, FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
    }

    private Component payErrorMessage(String key, EscMenuFlows flows) {
        return switch (key) {
            case "empty" -> DialogComponentUtils.parse("&cInforme um valor.");
            case "non_positive" -> DialogComponentUtils.parse("&cO valor deve ser maior que zero.");
            case "below_min" -> DialogComponentUtils.parse("&cValor mínimo: &f$" + formatDisplayAmount(flows.payMin()));
            case "above_max" -> DialogComponentUtils.parse("&cValor máximo: &f$" + formatDisplayAmount(flows.payMax()));
            default -> DialogComponentUtils.parse("&cValor inválido.");
        };
    }

    private static String formatDisplayAmount(double amount) {
        if (amount == Math.rint(amount)) {
            return String.format("%.0f", amount);
        }
        return String.format("%.2f", amount);
    }

    private static String formatCommandAmount(double amount) {
        return formatDisplayAmount(amount).replace(",", ".");
    }
}