package anarchy.minesky.core.api.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HeadURLResolver {

    // ConcurrentHashMap é obrigatório para evitar race conditions no Folia
    private static final Map<String, Component> CACHE = new ConcurrentHashMap<>();

    private HeadURLResolver() {}

    /**
     * Tag MiniMessage {@code <head_url:...>} — URL de textura ou hash Mojang.
     */
    public static TagResolver headUrlTag() {
        return createHeadUrlResolver();
    }

    /**
     * Tag {@code <head:uuid|nick>} — cabeça do jogador (textura do perfil Bukkit).
     */
    public static TagResolver headPlayerTag() {
        return TagResolver.resolver("head", (args, ctx) -> {
            String spec = args.popOr("UUID ou nick ausente").value();
            Component head = buildHeadFromPlayerSpecifier(spec);
            return Tag.selfClosingInserting(head);
        });
    }

    /**
     * Resolvers usados em mensagens MineSky (URLs + cabeças por jogador).
     */
    public static TagResolver mineSkyMiniMessageTags() {
        return TagResolver.builder()
                .resolver(headUrlTag())
                .resolver(headPlayerTag())
                .build();
    }

    public static Component deserializeMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        try {
            return MiniMessage.miniMessage().deserialize(input, mineSkyMiniMessageTags());
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    private static TagResolver createHeadUrlResolver() {
        return TagResolver.resolver("head_url", (args, ctx) -> {
            String input = args.popOr("URL ou Hash ausente").value();

            // Normaliza para a URL canônica aceita pelo cliente do Minecraft
            String textureUrl;
            if (input.startsWith("http://") || input.startsWith("https://")) {
                textureUrl = input;
            } else {
                textureUrl = "http://textures.minecraft.net/texture/" + input;
            }

            // computeIfAbsent atômico: seguro para chamadas paralelas de múltiplas regiões
            Component head = CACHE.computeIfAbsent(textureUrl, HeadURLResolver::buildHeadComponent);
            return Tag.selfClosingInserting(head);
        });
    }

    private static Component buildHeadFromPlayerSpecifier(String spec) {
        if (spec == null || spec.isBlank()) {
            return Component.empty();
        }
        spec = spec.trim();
        OfflinePlayer offline;
        try {
            UUID uuid = UUID.fromString(spec);
            offline = Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException e) {
            offline = Bukkit.getOfflinePlayer(spec);
        }
        String url = textureUrlFromProfile((PlayerProfile) offline.getPlayerProfile());
        if (url == null || url.isEmpty()) {
            return Component.empty();
        }
        return CACHE.computeIfAbsent(url, HeadURLResolver::buildHeadComponent);
    }

    private static String textureUrlFromProfile(PlayerProfile profile) {
        if (profile == null) {
            return null;
        }
        for (ProfileProperty prop : profile.getProperties()) {
            if (!"textures".equals(prop.getName())) {
                continue;
            }
            try {
                String decoded = new String(Base64.getDecoder().decode(prop.getValue()), StandardCharsets.UTF_8);
                int skin = decoded.indexOf("\"SKIN\"");
                if (skin < 0) {
                    continue;
                }
                int urlKey = decoded.indexOf("\"url\"", skin);
                if (urlKey < 0) {
                    continue;
                }
                int colon = decoded.indexOf(':', urlKey);
                int start = decoded.indexOf('"', colon + 1);
                if (start < 0) {
                    continue;
                }
                start++;
                int end = decoded.indexOf('"', start);
                if (end > start) {
                    return decoded.substring(start, end);
                }
            } catch (IllegalArgumentException ignored) {
                // perfil sem textura válida
            }
        }
        return null;
    }

    private static Component buildHeadComponent(String textureUrl) {
        try {
            // Monta o JSON padrão de skin da Mojang
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
            String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            // Cria o PlayerHeadObjectContents puramente com a Adventure API
            PlayerHeadObjectContents headContents = ObjectContents.playerHead()
                    .profileProperty(PlayerHeadObjectContents.property("textures", base64))
                    .build();

            return Component.object(headContents);
        } catch (Exception e) {
            return Component.text("[Erro na Cabeça]");
        }
    }
}
