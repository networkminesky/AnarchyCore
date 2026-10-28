package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.warps.WarpManager;
import anarchy.minesky.core.impl.warps.WarpSettings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EditWarpCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final WarpManager warps;

    public EditWarpCommand(MessagesManager messages, WarpManager warps) {
        this.messages = messages;
        this.warps = warps;
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
        if (args.length != 1) {
            sender.sendMessage(messages.component("warps.usage.editwarp"));
            return true;
        }
        warps.beginEditWarp(player, args[0]);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1 || !sender.hasPermission(WarpSettings.ADMIN_PERMISSION)) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (var warp : warps.storage().all()) {
            if (warp.id().startsWith(prefix)) {
                out.add(warp.id());
            }
        }
        return out;
    }
}
