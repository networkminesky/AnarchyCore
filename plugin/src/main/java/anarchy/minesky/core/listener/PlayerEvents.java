package anarchy.minesky.core.listener;

import anarchy.minesky.core.AnarchyCore;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.utils.Utils;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class PlayerEvents implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(null);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        final Player p = e.getPlayer();

        String prefix = "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
        }
        final Component name = DialogComponentUtils.parse(prefix).append(p.displayName());

        final Component chat = Component.text("<")
                .append(name)
                .append(Component.text(">"))
                .append(Component.space())
                .append(e.message());

        e.setCancelled(true);
        Bukkit.broadcast(chat);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String message = e.getMessage();
        if (message.isEmpty() || !message.startsWith("/")) return;

        // 1. Separa apenas o comando dos argumentos
        // Ex: "/TeLL Notch Oi" -> commandPart = "/tell", argsPart = " Notch Oi"
        String[] parts = message.split(" ", 2);
        String commandPart = parts[0].toLowerCase(); // Apenas o comando em lowercase
        String argsPart = parts.length > 1 ? " " + parts[1] : "";

        // Atualiza a mensagem para o Bukkit processar o comando em minúsculo
        e.setMessage(commandPart + argsPart);

        // 2. Extrai o comando sem a barra "/"
        String cleanCmd = commandPart.substring(1);

        // 3. Trata comandos com sintaxe de dois pontos (ex: /minecraft:me ou /essentials:spawn)
        if (cleanCmd.contains(":")) {
            String[] splitColon = cleanCmd.split(":", 2);
            String realCmd = splitColon[1]; // O comando sem o prefixo (ex: "spawn")

            if (!e.getPlayer().hasPermission("mineskycore.hiddensyntax." + realCmd)) {
                // Redireciona para o comando sem prefixo mantendo os argumentos, sem precisar re-executar chat()
                e.setMessage("/" + realCmd + argsPart);
                //Bukkit.getLogger().warning("O jogador " + e.getPlayer().getName() + " tentou utilizar sintaxe oculta: " + message + " e foi redirecionado.");
                cleanCmd = realCmd; // Atualiza para cair na checagem de /pl abaixo se for o caso
            } else {
                //Bukkit.getLogger().l.warning("O jogador " + e.getPlayer().getName() + " tentou utilizar sintaxe oculta: " + message + " e foi permitido.");
                cleanCmd = realCmd;
            }
        }

        // 4. Bloqueio e customização do /pl e /plugins
        if ((cleanCmd.equals("pl") || cleanCmd.equals("plugins"))
                && !e.getPlayer().hasPermission("anarchycore.seeplugins")) {

            final List<String> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                    .map(Plugin::getName)
                    .filter(b -> b.toLowerCase().contains("anarchy"))
                    .toList();

            e.getPlayer().sendMessage(Utils.hex("&#ed8106MineSky Plugins (" + plugins.size() + "):\n &8- &a" + String.join("&f, &a", plugins)));
            e.setCancelled(true);
        }
    }
}
