package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOptionSpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigCategorySpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigKeys;
import anarchy.minesky.core.api.playerconfig.PlayerConfigRegistry;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.config.MessagesManager;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

/**
 * Registra categorias e opções padrão do AnarchyCore via {@link PlayerConfigRegistry}.
 */
public final class CorePlayerConfigBootstrap {

    private CorePlayerConfigBootstrap() {}

    public static void register(Plugin plugin, PlayerConfigRegistry registry, MessagesManager messages) {
        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_TPA)
                .title(messages.component("config.dialog.categories.tpa.title"))
                .buttonLabel(messages.component("config.dialog.categories.tpa.button"))
                .iconMaterial(Material.ENDER_PEARL)
                .sortOrder(10)
                .build());

        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_TELL)
                .title(messages.component("config.dialog.categories.tell.title"))
                .buttonLabel(messages.component("config.dialog.categories.tell.button"))
                .iconMaterial(Material.WRITABLE_BOOK)
                .sortOrder(20)
                .build());

        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_FRIENDS)
                .title(messages.component("config.dialog.categories.friends.title"))
                .buttonLabel(messages.component("config.dialog.categories.friends.button"))
                .iconMaterial(Material.PLAYER_HEAD)
                .sortOrder(30)
                .build());

        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_CHAT)
                .title(messages.component("config.dialog.categories.chat.title"))
                .buttonLabel(messages.component("config.dialog.categories.chat.button"))
                .iconMaterial(Material.WRITTEN_BOOK)
                .sortOrder(40)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.TPA_FROM_STRANGERS, PlayerConfigKeys.CATEGORY_TPA)
                .label(DialogComponentUtils.parse(messages.raw("config.options.tpa-strangers")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.TPAHERE_FROM_STRANGERS, PlayerConfigKeys.CATEGORY_TPA)
                .label(DialogComponentUtils.parse(messages.raw("config.options.tpahere-strangers")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.TELL_FROM_STRANGERS, PlayerConfigKeys.CATEGORY_TELL)
                .label(DialogComponentUtils.parse(messages.raw("config.options.tell-strangers")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS, PlayerConfigKeys.CATEGORY_TELL)
                .label(DialogComponentUtils.parse(messages.raw("config.options.tell-sounds")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.FRIENDS_ALLOW_INCOMING_REQUESTS, PlayerConfigKeys.CATEGORY_FRIENDS)
                .label(DialogComponentUtils.parse(messages.raw("config.options.friends-allow-requests")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.FRIENDS_REQUEST_SOUNDS, PlayerConfigKeys.CATEGORY_FRIENDS)
                .label(DialogComponentUtils.parse(messages.raw("config.options.friends-request-sounds")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.FRIENDS_NOTIFY_ON_FRIEND_JOIN, PlayerConfigKeys.CATEGORY_FRIENDS)
                .label(DialogComponentUtils.parse(messages.raw("config.options.friends-notify-join")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.FRIENDS_JOIN_SOUNDS, PlayerConfigKeys.CATEGORY_FRIENDS)
                .label(DialogComponentUtils.parse(messages.raw("config.options.friends-join-sounds")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.CHAT_GLOBAL_ENABLED, PlayerConfigKeys.CATEGORY_CHAT)
                .label(DialogComponentUtils.parse(messages.raw("config.options.chat-global-enabled")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.CHAT_HEADS_ENABLED, PlayerConfigKeys.CATEGORY_CHAT)
                .label(DialogComponentUtils.parse(messages.raw("config.options.chat-heads-enabled")))
                .defaultValue(false)
                .build());
    }
}
