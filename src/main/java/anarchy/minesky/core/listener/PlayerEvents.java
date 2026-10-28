package anarchy.minesky.core.listener;

import anarchy.minesky.core.dialogapi.DialogComponentUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

        String prefix = PlaceholderAPI.setPlaceholders(p, "%luckperms_prefix%");
        final Component name = DialogComponentUtils.parse(prefix).append(p.displayName());

        final Component chat = Component.text("<")
                .append(name)
                .append(Component.text(">"))
                .append(Component.space())
                .append(e.message());

        e.setCancelled(true);
        Bukkit.broadcast(chat);
    }
}
