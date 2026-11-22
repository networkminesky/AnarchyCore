package anarchy.minesky.core.impl.escmenu.flows;

import anarchy.minesky.core.impl.profile.PlayerProfileFlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public final class EscHubStatsFlow {

    private final Plugin plugin;
    private final OnlinePlayerGridDialog playerGrid;
    private final PlayerProfileFlow profiles;
    private final Consumer<Player> openHub;

    public EscHubStatsFlow(
            Plugin plugin,
            OnlinePlayerGridDialog playerGrid,
            PlayerProfileFlow profiles,
            Consumer<Player> openHub) {
        this.plugin = plugin;
        this.playerGrid = playerGrid;
        this.profiles = profiles;
        this.openHub = openHub;
    }

    public void start(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        java.util.function.Consumer<Player> back = p -> p.getScheduler().run(plugin, task -> openHub.accept(p), null);
        playerGrid.open(
                viewer,
                Component.text("Stats — escolha um jogador", NamedTextColor.GOLD),
                target -> profiles.openOnlineTarget(viewer, target, () -> start(viewer)),
                back,
                true,
                nick -> profiles.openOfflineName(viewer, nick, () -> start(viewer)));
    }
}