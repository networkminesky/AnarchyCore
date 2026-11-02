package anarchy.minesky.core.listener;

import anarchy.minesky.core.AnarchyCore;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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

    /**
     * Roda antes do jogador existir no mundo (100% assíncrono).
     * Injeta as texturas no PlayerProfile da sessão.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String playerName = event.getName();

        try {
            PlayerStorage playerStorage = skinsRestorerAPI.getPlayerStorage();
            SkinStorage skinStorage = skinsRestorerAPI.getSkinStorage();

            // 1. Tenta pegar do cache local / banco de dados (0ms de atraso)
            Optional<SkinProperty> skinOpt = playerStorage.getSkinOfPlayer(uuid);

            // 2. Primeira vez do jogador: consulta a Mojang e salva o vínculo para sempre
            if (skinOpt.isEmpty()) {
                Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(playerName);
                if (result.isPresent()) {
                    InputDataResult data = result.get();
                    playerStorage.setSkinIdOfPlayer(uuid, data.getIdentifier());
                    skinOpt = Optional.of(data.getProperty());
                }
            }

            // 3. Injeta a textura diretamente no perfil nativo do Paper/Folia antes de logar
            if (skinOpt.isPresent()) {
                SkinProperty property = skinOpt.get();
                PlayerProfile profile = event.getPlayerProfile();
                profile.setProperty(new ProfileProperty("textures", property.getValue(), property.getSignature()));
                event.setPlayerProfile(profile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar perfil no PreLogin para " + playerName + ": " + e.getMessage());
        }
    }

    /**
     * Fallback para garantir compatibilidade caso algum plugin externo
     * precise que o método da API do SkinsRestorer seja chamado.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Se o profile já tem texturas (injetadas com sucesso no PreLogin), não precisa fazer nada!
        if (player.getPlayerProfile().hasProperty("textures")) {
            return;
        }

        // Caso tenha falhado no PreLogin, tenta aplicar assincronamente como contingência
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            try {
                skinsRestorerAPI.getSkinApplier(Player.class).applySkin(player);
            } catch (Exception ignored) {}
        }, null, 5L);
    }
}