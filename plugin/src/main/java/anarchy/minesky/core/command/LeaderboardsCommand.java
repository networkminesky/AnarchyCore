package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.leaderboards.LeaderboardDialog;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class LeaderboardsCommand implements CommandExecutor {

    private final Plugin plugin;
    private final MessagesManager messages;
    private final LeaderboardDialog leaderboards;

    public LeaderboardsCommand(Plugin plugin, MessagesManager messages, LeaderboardDialog leaderboards) {
        this.plugin = plugin;
        this.messages = messages;
        this.leaderboards = leaderboards;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.component("commands.player-only"));
            return true;
        }
        player.getScheduler().run(plugin, task -> leaderboards.openMain(player), null);
        return true;
    }
}