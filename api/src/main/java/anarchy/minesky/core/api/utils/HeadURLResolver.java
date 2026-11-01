package anarchy.minesky.core.api.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HeadURLResolver {

    // ConcurrentHashMap é obrigatório para evitar race conditions no Folia
    private static final Map<String, Component> CACHE = new ConcurrentHashMap<>();

    private HeadURLResolver() {}

    /**
     * Tag MiniMessage {@code <head_url:...>} — URL de textura ou hash Mojang.
     */
    public static TagResolver headUrlTag() {
        return create();
    }

    public static Component deserializeMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        try {
            return MiniMessage.miniMessage().deserialize(input, headUrlTag());
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    public static TagResolver create() {
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
