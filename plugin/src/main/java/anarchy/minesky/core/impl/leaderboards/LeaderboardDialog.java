package anarchy.minesky.core.impl.leaderboards;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardResult;
import anarchy.minesky.core.impl.profile.PlayerProfileFlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.BiConsumer;

public final class LeaderboardDialog {

    private static final int BUTTON_WIDTH = 155;
    private static final int BEDROCK_FULL_PAGE = 40;

    private final Plugin plugin;
    private final DialogManager dialogManager;
    private final LeaderboardService service;
    private final PlayerProfileFlow profiles;

    public LeaderboardDialog(
            Plugin plugin,
            DialogManager dialogManager,
            LeaderboardService service,
            PlayerProfileFlow profiles) {
        this.plugin = plugin;
        this.dialogManager = dialogManager;
        this.service = service;
        this.profiles = profiles;
    }

    public void openMain(Player player) {
        openMain(player, null);
    }

    public void openMain(Player player, Runnable back) {
        MineSkyDialog.Builder builder = MineSkyDialog.builder(
                        DialogComponentUtils.parse("&f&lLeaderboards"))
                .body(Component.text("Escolha uma categoria", NamedTextColor.GRAY))
                .multiActionColumns(3);

        for (LeaderboardCategory category : LeaderboardCategory.values()) {
            builder.button(DialogButton.builder()
                    .label(category.buttonLabel())
                    .width(BUTTON_WIDTH)
                    .closeOnClick(false)
                    .onClick(p -> openCategoryPreview(p, category, LeaderboardPeriod.ALL_TIME, () -> openMain(p, back)))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(Component.text("Voltar", NamedTextColor.GRAY))
                .width(BUTTON_WIDTH)
                .onClick(p -> {
                    if (back != null) {
                        back.run();
                    } else {
                        p.closeDialog();
                    }
                })
                .build());

        builder.show(player);
    }

    public void openCategory(Player player, LeaderboardCategory category) {
        openCategoryPreview(player, category, LeaderboardPeriod.ALL_TIME, () -> openMain(player));
    }

    private void openCategoryPreview(
            Player player, LeaderboardCategory category, LeaderboardPeriod period, Runnable backToMain) {
        service.fetchCategoryPreview(category, period, player, result -> showCategory(
                player, category, period, result, false, 0, backToMain));
    }

    private void openCategoryFull(
            Player player, LeaderboardCategory category, LeaderboardPeriod period, Runnable backToPreview) {
        service.fetchCategoryFull(category, period, player, result -> showCategory(
                player, category, period, result, true, 0, backToPreview));
    }

    private void showCategory(
            Player player,
            LeaderboardCategory category,
            LeaderboardPeriod period,
            LeaderboardResult result,
            boolean fullView,
            int bedrockPage,
            Runnable backAction) {
        LeaderboardPeriod activePeriod = result.period() != null ? result.period() : period;
        Component title = Component.text(
                fullView
                        ? LeaderboardFormatter.fullTitle(category, activePeriod)
                        : LeaderboardFormatter.previewTitle(category, activePeriod),
                NamedTextColor.WHITE);
        MineSkyDialog.Builder builder = MineSkyDialog.builder(title);

        boolean bedrock = dialogManager.isBedrock(player);
        Runnable profileBack = fullView
                ? backAction
                : () -> openCategoryPreview(player, category, activePeriod, backAction);
        BiConsumer<Player, LeaderboardLine> profileOpener = (viewer, line) -> profiles.openFromLeaderboard(
                viewer, line.uuid(), line.name(), profileBack);

        if (!result.ok()) {
            builder.body(Component.text(result.error(), NamedTextColor.RED));
        } else if (result.top().isEmpty()) {
            builder.body(Component.text("Nenhum dado ainda.", NamedTextColor.GRAY));
        } else if (fullView && bedrock) {
            appendBedrockPage(player, builder, category, result.top(), bedrockPage, profileOpener);
        } else {
            for (LeaderboardLine line : result.top()) {
                builder.body(LeaderboardLineRenderer.renderLine(
                        line, category, player, bedrock, plugin, profileOpener));
            }
        }

        if (result.ok() && result.viewerValue() > 0) {
            String selfRank = result.viewerRank() > 0
                    ? "#" + result.viewerRank()
                    : "Fora do top";
            builder.body(Component.text("Você: " + selfRank + " — ", NamedTextColor.GRAY)
                    .append(LeaderboardFormatter.compactValueComponent(category, result.viewerValue())));
        }

        boolean periodToggle = category.supportsWeekly();
        if (!fullView) {
            builder.multiActionColumns(periodToggle ? 3 : 2);
            builder.button(DialogButton.builder()
                    .label(Component.text("Voltar", NamedTextColor.GRAY))
                    .width(BUTTON_WIDTH)
                    .closeOnClick(false)
                    .onClick(p -> backAction.run())
                    .build());
            if (periodToggle) {
                LeaderboardPeriod other = activePeriod.toggle();
                builder.button(DialogButton.builder()
                        .label(Component.text(LeaderboardFormatter.periodToggleLabel(activePeriod), NamedTextColor.AQUA))
                        .width(BUTTON_WIDTH)
                        .closeOnClick(false)
                        .onClick(p -> openCategoryPreview(p, category, other, backAction))
                        .build());
            }
            builder.button(DialogButton.builder()
                    .label(Component.text("Ver inteira", NamedTextColor.GREEN))
                    .width(BUTTON_WIDTH)
                    .closeOnClick(false)
                    .onClick(p -> openCategoryFull(
                            p,
                            category,
                            activePeriod,
                            () -> openCategoryPreview(p, category, activePeriod, backAction)))
                    .build());
        } else {
            int columns = periodToggle ? 2 : 1;
            if (bedrock && result.ok() && result.top().size() > BEDROCK_FULL_PAGE) {
                columns = Math.max(columns, periodToggle ? 3 : 2);
            }
            builder.multiActionColumns(columns);
            if (bedrock && result.ok() && result.top().size() > BEDROCK_FULL_PAGE) {
                int pages = (result.top().size() + BEDROCK_FULL_PAGE - 1) / BEDROCK_FULL_PAGE;
                if (bedrockPage > 0) {
                    int prev = bedrockPage - 1;
                    builder.button(DialogButton.builder()
                            .label(Component.text("« Anterior", NamedTextColor.GRAY))
                            .width(BUTTON_WIDTH)
                            .closeOnClick(false)
                            .onClick(p -> service.fetchCategoryFull(category, activePeriod, p, r -> showCategory(
                                    p, category, activePeriod, r, true, prev, backAction)))
                            .build());
                }
                if (bedrockPage + 1 < pages) {
                    int next = bedrockPage + 1;
                    builder.button(DialogButton.builder()
                            .label(Component.text("Próximo »", NamedTextColor.GRAY))
                            .width(BUTTON_WIDTH)
                            .closeOnClick(false)
                            .onClick(p -> service.fetchCategoryFull(category, activePeriod, p, r -> showCategory(
                                    p, category, activePeriod, r, true, next, backAction)))
                            .build());
                }
            }
            builder.button(DialogButton.builder()
                    .label(Component.text("Voltar", NamedTextColor.GRAY))
                    .width(BUTTON_WIDTH)
                    .closeOnClick(false)
                    .onClick(p -> backAction.run())
                    .build());
            if (periodToggle) {
                LeaderboardPeriod other = activePeriod.toggle();
                builder.button(DialogButton.builder()
                        .label(Component.text(LeaderboardFormatter.periodToggleLabel(activePeriod), NamedTextColor.AQUA))
                        .width(BUTTON_WIDTH)
                        .closeOnClick(false)
                        .onClick(p -> openCategoryFull(
                                p,
                                category,
                                other,
                                () -> openCategoryPreview(p, category, other, backAction)))
                        .build());
            }
        }

        builder.show(player);
    }

    private void appendBedrockPage(
            Player player,
            MineSkyDialog.Builder builder,
            LeaderboardCategory category,
            List<LeaderboardLine> lines,
            int page,
            BiConsumer<Player, LeaderboardLine> profileOpener) {
        int from = page * BEDROCK_FULL_PAGE;
        int to = Math.min(from + BEDROCK_FULL_PAGE, lines.size());
        if (from >= lines.size()) {
            from = 0;
            to = Math.min(BEDROCK_FULL_PAGE, lines.size());
        }
        builder.body(Component.text("Página " + (page + 1), NamedTextColor.GRAY));
        for (int i = from; i < to; i++) {
            LeaderboardLine line = lines.get(i);
            builder.body(LeaderboardLineRenderer.renderLine(
                    line, category, player, true, plugin, profileOpener));
        }
    }
}