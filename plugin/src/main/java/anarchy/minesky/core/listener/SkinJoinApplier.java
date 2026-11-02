package anarchy.minesky.core.listener;

import anarchy.minesky.core.AnarchyCore;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;
import java.util.UUID;

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

        // 1. Se o jogador já possui texturas válidas (ex: original / enviado pelo proxy), não reaplica
        if (!player.getPlayerProfile().getTextures().isEmpty()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        // 2. Executa a checagem no Scheduler Assíncrono do Folia (não trava o tick do mundo nem de entidades)
        Bukkit.getAsyncScheduler().runNow(plugin, asyncTask -> {
            try {
                PlayerStorage playerStorage = skinsRestorerAPI.getPlayerStorage();
                SkinStorage skinStorage = skinsRestorerAPI.getSkinStorage();

                // 3. Tenta buscar a skin já vinculada/armazenada no cache/banco local
                Optional<SkinProperty> skinOpt = playerStorage.getSkinOfPlayer(uuid);

                // Se NÃO estiver no cache local (primeira vez que o jogador entra)
                if (skinOpt.isEmpty()) {
                    Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(playerName);
                    if (result.isPresent()) {
                        InputDataResult data = result.get();
                        // SALVA o vínculo no banco do SkinsRestorer para que NUNCA MAIS consulte a Mojang nos próximos joins
                        playerStorage.setSkinIdOfPlayer(uuid, data.getIdentifier());
                        skinOpt = Optional.of(data.getProperty());
                    }
                }

                // 4. Se encontrou a skin (seja do cache local ou recém-baixada)
                if (skinOpt.isPresent()) {
                    SkinProperty skinProperty = skinOpt.get();

                    // Agenda a aplicação de volta na thread da entidade do jogador
                    player.getScheduler().runDelayed(plugin, entityTask -> {
                        if (!player.isOnline()) return;

                        try {
                            // Aplica DIRETAMENTE a propriedade resolvida (evita requisições à Mojang)
                            skinsRestorerAPI.getSkinApplier(Player.class).applySkin(player, skinProperty);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Erro ao aplicar skin para " + playerName + ": " + e.getMessage());
                        }
                    }, null, 5L); // 5 ticks (~250ms) é suficiente para o cliente terminar de spawnar
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro no processamento de skin para " + playerName + ": " + e.getMessage());
            }
        });
    }
}