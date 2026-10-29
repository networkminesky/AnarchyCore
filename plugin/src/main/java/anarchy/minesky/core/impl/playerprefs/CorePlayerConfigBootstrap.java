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

        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_MONEY)
                .title(messages.component("config.dialog.categories.money.title"))
                .buttonLabel(messages.component("config.dialog.categories.money.button"))
                .iconMaterial(Material.GOLD_INGOT)
                .sortOrder(20)
                .build());

        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_TELL)
                .title(messages.component("config.dialog.categories.tell.title"))
                .buttonLabel(messages.component("config.dialog.categories.tell.button"))
                .iconMaterial(Material.WRITABLE_BOOK)
                .sortOrder(30)
                .build());

        registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(PlayerConfigKeys.CATEGORY_SCOREBOARD)
                .title(messages.component("config.dialog.categories.scoreboard.title"))
                .buttonLabel(messages.component("config.dialog.categories.scoreboard.button"))
                .iconMaterial(Material.OAK_SIGN)
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
                        PlayerConfigKeys.PAY_FROM_STRANGERS, PlayerConfigKeys.CATEGORY_MONEY)
                .label(DialogComponentUtils.parse(messages.raw("config.options.pay-strangers")))
                .defaultValue(true)
                .build());

        registry.registerBooleanOption(plugin, PlayerConfigBooleanOptionSpec.builder(
                        PlayerConfigKeys.PAYMENT_SOUNDS, PlayerConfigKeys.CATEGORY_MONEY)
                .label(DialogComponentUtils.parse(messages.raw("config.options.payment-sounds")))
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
                        PlayerConfigKeys.SCOREBOARD_ENABLED, PlayerConfigKeys.CATEGORY_SCOREBOARD)
                .label(DialogComponentUtils.parse(messages.raw("config.options.scoreboard-enabled")))
                .defaultValue(true)
                .build());
    }
}
