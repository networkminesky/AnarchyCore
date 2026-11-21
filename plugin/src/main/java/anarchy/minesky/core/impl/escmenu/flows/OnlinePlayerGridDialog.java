package anarchy.minesky.core.impl.escmenu.flows;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.api.utils.HeadURLResolver;
import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.escmenu.EscMenuFlows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public final class OnlinePlayerGridDialog {

    private static final String MANUAL_NICK_KEY = "player_nick";

    private final DialogManager dialog;
    private final EscMenuFlows flows;

    public OnlinePlayerGridDialog(DialogManager dialog, EscMenuFlows flows) {
        this.dialog = dialog;
        this.flows = flows;
    }

    public void open(Player viewer, Component title, Consumer<Player> onSelected, Consumer<Player> onBack) {
        open(viewer, title, onSelected, onBack, false, null);
    }

    public void open(
            Player viewer,
            Component title,
            Consumer<Player> onSelected,
            Consumer<Player> onBack,
            boolean showSelfProfile,
            Consumer<String> onOfflineNick) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        List<Player> visible = VisibleOnlinePlayers.sortedVisible(viewer);
        if (visible.isEmpty()) {
            openEmpty(viewer, title, onSelected, onBack, showSelfProfile, onOfflineNick);
            return;
        }
        openPage(viewer, title, visible, 0, onSelected, onBack, showSelfProfile, onOfflineNick);
    }

    private void openPage(
            Player viewer,
            Component title,
            List<Player> all,
            int page,
            Consumer<Player> onSelected,
            Consumer<Player> onBack,
            boolean showSelfProfile,
            Consumer<String> onOfflineNick) {
        int pageSize = flows.selectorPageSize();
        int from = page * pageSize;
        if (from >= all.size()) {
            openPage(viewer, title, all, 0, onSelected, onBack, showSelfProfile, onOfflineNick);
            return;
        }
        int to = Math.min(from + pageSize, all.size());
        List<Player> slice = all.subList(from, to);
        boolean hasNext = to < all.size();
        boolean hasPrev = page > 0;

        MineSkyDialog.Builder builder = MineSkyDialog.builder(title)
                .body(Component.text(
                        "Jogadores online: " + all.size() + (all.size() > pageSize ? " (pág. " + (page + 1) + ")" : ""),
                        NamedTextColor.GRAY))
                .multiActionColumns(2);

        boolean bedrock = dialog.isBedrock(viewer);
        int width = flows.selectorButtonWidth();
        if (showSelfProfile) {
            builder.button(DialogButton.builder()
                    .label(Component.text("★ Meu Perfil", NamedTextColor.GREEN))
                    .width(width)
                    .onClick(p -> onSelected.accept(viewer))
                    .build());
        }
        for (Player target : slice) {
            Player captured = target;
            builder.button(DialogButton.builder()
                    .label(playerLabel(bedrock, captured))
                    .width(width)
                    .onClick(p -> onSelected.accept(captured))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(Component.text("+ Digitar nick", NamedTextColor.GRAY))
                .width(width)
                .onClick(p -> openManualNick(p, title, onSelected, onBack, showSelfProfile, onOfflineNick))
                .build());

        if (hasPrev) {
            int prevPage = page - 1;
            builder.button(DialogButton.builder()
                    .label(Component.text("« Anterior", NamedTextColor.GRAY))
                    .width(width)
                    .onClick(p -> openPage(p, title, all, prevPage, onSelected, onBack, showSelfProfile, onOfflineNick))
                    .build());
        }
        if (hasNext) {
            int nextPage = page + 1;
            builder.button(DialogButton.builder()
                    .label(Component.text("Próximo »", NamedTextColor.GRAY))
                    .width(width)
                    .onClick(p -> openPage(p, title, all, nextPage, onSelected, onBack, showSelfProfile, onOfflineNick))
                    .build());
        }

        if (onBack != null) {
            builder.button(DialogButton.builder()
                    .label(Component.text("Voltar", NamedTextColor.GRAY))
                    .width(width)
                    .onClick(onBack)
                    .build());
        }

        builder.show(viewer);
    }

    private void openEmpty(
            Player viewer,
            Component title,
            Consumer<Player> onSelected,
            Consumer<Player> onBack,
            boolean showSelfProfile,
            Consumer<String> onOfflineNick) {
        int width = flows.selectorButtonWidth();
        MineSkyDialog.Builder builder = MineSkyDialog.builder(title)
                .body(Component.text("Nenhum jogador disponível no momento.", NamedTextColor.RED))
                .multiActionColumns(1);
        if (showSelfProfile) {
            builder.button(DialogButton.builder()
                    .label(Component.text("★ Meu Perfil", NamedTextColor.GREEN))
                    .width(240)
                    .onClick(p -> onSelected.accept(viewer))
                    .build());
        }
        builder.button(DialogButton.builder()
                .label(Component.text("+ Digitar nick manualmente", NamedTextColor.GRAY))
                .width(240)
                .onClick(p -> openManualNick(p, title, onSelected, onBack, showSelfProfile, onOfflineNick))
                .build());
        if (onBack != null) {
            builder.button(DialogButton.builder()
                    .label(Component.text("Voltar", NamedTextColor.GRAY))
                    .width(width)
                    .onClick(onBack)
                    .build());
        }
        builder.show(viewer);
    }

    private void openManualNick(
            Player viewer,
            Component title,
            Consumer<Player> onSelected,
            Consumer<Player> onBack,
            boolean showSelfProfile,
            Consumer<String> onOfflineNick) {
        MineSkyDialog.builder(title)
                .body(Component.text("Digite o nick exato do jogador.", NamedTextColor.GRAY))
                .addInput(TextInput.builder(MANUAL_NICK_KEY, Component.text("Nick"))
                        .placeholder("Nome do jogador")
                        .maxLength(16)
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Confirmar"))
                        .width(180)
                        .closeOnClick(false)
                        .onClick((p, res) -> onManualNick(
                                p, res, title, onSelected, onBack, showSelfProfile, onOfflineNick))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Voltar"))
                        .width(150)
                        .onClick(p -> open(p, title, onSelected, onBack, showSelfProfile, onOfflineNick))
                        .build())
                .show(viewer);
    }

    private void onManualNick(
            Player viewer,
            DialogResponse response,
            Component title,
            Consumer<Player> onSelected,
            Consumer<Player> onBack,
            boolean showSelfProfile,
            Consumer<String> onOfflineNick) {
        String nick = response.getText(MANUAL_NICK_KEY, "").trim();
        var resolved = VisibleOnlinePlayers.resolveVisible(viewer, nick);
        if (resolved.isEmpty()) {
            if (onOfflineNick != null && !nick.isBlank()) {
                viewer.closeDialog();
                onOfflineNick.accept(nick);
                return;
            }
            viewer.closeDialog();
            viewer.sendMessage(DialogComponentUtils.parse("&cJogador indisponível, invisível ou nick inválido."));
            open(viewer, title, onSelected, onBack, showSelfProfile, onOfflineNick);
            return;
        }
        viewer.closeDialog();
        onSelected.accept(resolved.get());
    }

    static Component playerLabel(boolean bedrockViewer, Player target) {
        Component name = Component.text(target.getName(), NamedTextColor.WHITE);
        if (bedrockViewer) {
            return name;
        }
        Component head = HeadURLResolver.playerHeadFor(target.getUniqueId().toString());
        if (head == null || head.equals(Component.empty())) {
            return name;
        }
        return head.append(Component.space()).append(name);
    }

    static Component targetBody(boolean bedrockViewer, Player target) {
        if (bedrockViewer) {
            return Component.text(target.getName(), NamedTextColor.WHITE);
        }
        Component head = HeadURLResolver.playerHeadFor(target.getUniqueId().toString());
        Component name = Component.text(target.getName(), NamedTextColor.WHITE);
        if (head == null || head.equals(Component.empty())) {
            return name;
        }
        return head.append(Component.space()).append(name);
    }
}