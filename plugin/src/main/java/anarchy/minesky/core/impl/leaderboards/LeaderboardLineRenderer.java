package anarchy.minesky.core.impl.leaderboards;

import anarchy.minesky.core.api.utils.HeadURLResolver;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

public final class LeaderboardLineRenderer {

    private LeaderboardLineRenderer() {}

    public static Component renderLine(
            LeaderboardLine line,
            LeaderboardCategory category,
            Player viewer,
            boolean bedrock,
            Plugin plugin,
            BiConsumer<Player, LeaderboardLine> profileOpener) {
        TextColors colors = resolveColors(line.position());
        Component positionPart = Component.text("#" + line.position() + " ", colors.position());
        Component head = Component.empty();
        if (!bedrock) {
            Component resolved = HeadURLResolver.playerHeadFor(line.uuid().toString());
            if (resolved != null && !resolved.equals(Component.empty())) {
                head = playerHeadLinePart(resolved);
            }
        }
        Component namePart = Component.text(line.name(), colors.name());
        Component separator = Component.text(" - ", LeaderboardRankStyle.GRAY_RANK);
        Component valuePart = LeaderboardFormatter.compactValueComponent(category, line.value());
        Component built = positionPart.append(head).append(namePart).append(separator).append(valuePart);

        boolean clickable = LeaderboardInteractions.profileClickAllowed(
                viewer.getUniqueId(),
                line.uuid(),
                LeaderboardInteractions.hasViewOthersPermission(viewer));
        if (!clickable || profileOpener == null || plugin == null) {
            return built;
        }
        return built.hoverEvent(HoverEvent.showText(Component.text("Ver perfil", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.callback(
                        audience -> {
                            if (!(audience instanceof Player player)) {
                                return;
                            }
                            player.closeDialog();
                            player.getScheduler()
                                    .run(plugin, task -> profileOpener.accept(player, line), null);
                        },
                        ClickCallback.Options.builder().build()));
    }

    public static Component playerHeadLinePart(Component resolved) {
        if (resolved == null || resolved.equals(Component.empty())) {
            return Component.empty();
        }
        return resolved.color(NamedTextColor.WHITE).append(Component.space());
    }

    static TextColors resolveColors(int position) {
        return new TextColors(
                LeaderboardRankStyle.positionColor(position), LeaderboardRankStyle.nameColor(position));
    }

    record TextColors(net.kyori.adventure.text.format.TextColor position, net.kyori.adventure.text.format.TextColor name) {}
}