package anarchy.minesky.core.listener;

import anarchy.minesky.core.AnarchyCore;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SkinJoinApplier implements Listener {

    private final SkinsRestorer skinsRestorerAPI;
    private final AnarchyCore plugin;

    public SkinJoinApplier(AnarchyCore plugin) {
        this.skinsRestorerAPI = SkinsRestorerProvider.get();
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getScheduler().runDelayed(plugin, (task) -> {
            if(!player.isOnline())
                return;

            String playerName = player.getName();
            try {
                // O método applySkin e skinApplier lidam com a atualização do GameProfile do jogador
                skinsRestorerAPI.getSkinApplier(Player.class).applySkin(player, SkinIdentifier.ofCustom(playerName));
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao aplicar skin async para " + playerName + ": " + e.getMessage());
            }
        }, () -> {
            plugin.getLogger().info("Jogador saiu antes da aplicação da skin.");
        }, 40L);
    }

}
