package anarchy.minesky.core.impl.escmenu.flows;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.escmenu.EscMenuDefinition;
import anarchy.minesky.core.impl.escmenu.EscMenuFlows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class EscHubTpaFlow {

    private final Plugin plugin;
    private final DialogManager dialog;
    private final EscMenuDefinition definition;
    private final OnlinePlayerGridDialog playerGrid;
    private final Consumer<Player> openHub;

    public EscHubTpaFlow(
            Plugin plugin,
            DialogManager dialog,
            EscMenuDefinition definition,
            OnlinePlayerGridDialog playerGrid,
            Consumer<Player> openHub) {
        this.plugin = plugin;
        this.dialog = dialog;
        this.definition = definition;
        this.playerGrid = playerGrid;
        this.openHub = openHub;
    }

    public void start(Player viewer) {
        EscMenuFlows flows = definition.flows();
        playerGrid.open(
                viewer,
                Component.text("TPA — escolha um jogador", NamedTextColor.AQUA),
                target -> openConfirm(viewer, target),
                openHub);
    }

    private void openConfirm(Player viewer, Player target) {
        if (!VisibleOnlinePlayers.isValidTarget(viewer, target)) {
            viewer.sendMessage(DialogComponentUtils.parse("&cJogador não está mais disponível."));
            start(viewer);
            return;
        }
        boolean bedrock = dialog.isBedrock(viewer);
        EscMenuFlows flows = definition.flows();
        int width = flows.selectorButtonWidth();
        Component title = Component.text("Teleportar com ", NamedTextColor.WHITE)
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.text("?", NamedTextColor.WHITE));

        MineSkyDialog.builder(title)
                .body(OnlinePlayerGridDialog.targetBody(bedrock, target))
                .multiActionColumns(2)
                .button(DialogButton.builder()
                        .label(Component.text("Ir até", NamedTextColor.GREEN))
                        .width(width)
                        .onClick(p -> runTpaCommand(p, target, flows.tpaCommandTo()))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Puxar", NamedTextColor.GOLD))
                        .width(width)
                        .onClick(p -> runTpaCommand(p, target, flows.tpaCommandHere()))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Voltar", NamedTextColor.GRAY))
                        .width(width)
                        .closeOnClick(false)
                        .onClick(p -> start(p))
                        .build())
                .show(viewer);
    }

    private void runTpaCommand(Player viewer, Player target, String command) {
        if (!VisibleOnlinePlayers.isValidTarget(viewer, target)) {
            viewer.sendMessage(DialogComponentUtils.parse("&cJogador não está mais disponível."));
            return;
        }
        viewer.closeDialog();
        String base = command.startsWith("/") ? command.substring(1) : command;
        viewer.getScheduler().run(plugin, task -> {
            if (!viewer.isOnline()) {
                return;
            }
            Player live = viewer.getServer().getPlayer(target.getUniqueId());
            if (!VisibleOnlinePlayers.isValidTarget(viewer, live)) {
                viewer.sendMessage(DialogComponentUtils.parse("&cJogador não está mais disponível."));
                return;
            }
            viewer.performCommand(base + " " + live.getName());
        }, null);
    }
}