package anarchy.minesky.core.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Encapsula {@link CommandExecutor} / {@link TabCompleter} legados para {@link BasicCommand} (Paper plugin).
 */
public final class LegacyBukkitBasicCommand implements BasicCommand {

    private final String label;
    private final CommandExecutor executor;
    private final TabCompleter tabCompleter;
    private final Command shell;

    public LegacyBukkitBasicCommand(String label, CommandExecutor executor, TabCompleter tabCompleter) {
        this.label = label;
        this.executor = executor;
        this.tabCompleter = tabCompleter;
        this.shell = new ShellCommand(label);
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        executor.onCommand(source.getSender(), shell, label, args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (tabCompleter == null) {
            return List.of();
        }
        List<String> suggestions = tabCompleter.onTabComplete(source.getSender(), shell, label, args);
        return suggestions == null ? List.of() : suggestions;
    }

    private static final class ShellCommand extends Command {

        ShellCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return false;
        }
    }
}
