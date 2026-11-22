package anarchy.minesky.core.escmenu;

import anarchy.minesky.core.impl.escmenu.EscMenuButton;
import anarchy.minesky.core.impl.escmenu.EscMenuButtonType;
import anarchy.minesky.core.impl.escmenu.EscMenuConfigLoader;
import anarchy.minesky.core.impl.escmenu.EscMenuDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscMenuConfigLoaderTest {

    @Test
    void bundledDefaults_twoColumnsAndCoreButtons() throws IOException {
        EscMenuDefinition definition = EscMenuConfigLoader.loadFromResourceOnly();

        assertEquals(2, definition.columns());
        assertEquals(160, definition.buttonWidth());
        assertTrue(definition.commandEnabled());
        assertEquals("menu", definition.commandName());
        assertFalse(definition.exitEnabled());

        List<EscMenuButton> buttons = definition.buttons();
        assertTrue(buttons.size() >= 10, "grade padrão deve ter vários botões");

        Set<String> ids = buttons.stream().map(EscMenuButton::id).collect(Collectors.toSet());
        assertEquals(buttons.size(), ids.size(), "ids duplicados");

        EscMenuButton config = find(buttons, "config");
        assertEquals(EscMenuButtonType.CUSTOM, config.type());
        assertEquals("config", config.handler());

        EscMenuButton homes = find(buttons, "homes");
        assertEquals(EscMenuButtonType.COMMAND, homes.type());
        assertEquals("/home", homes.command());

        assertFalse(ids.contains("convite_status"));

        EscMenuButton tpa = find(buttons, "tpa");
        assertEquals(EscMenuButtonType.CUSTOM, tpa.type());
        assertEquals("tpa", tpa.handler());

        EscMenuButton pix = find(buttons, "pix");
        assertEquals(EscMenuButtonType.CUSTOM, pix.type());
        assertEquals("pay", pix.handler());

        assertEquals(4, definition.flows().payPresets().size());
    }

    @Test
    void duplicateIdRejected() {
        String yaml = """
                pause:
                  label: Test
                hub:
                  title: Test
                  columns: 2
                  button-width: 160
                command:
                  enabled: false
                  name: menu
                buttons:
                  - id: a
                    label: A
                    type: COMMAND
                    command: foo
                  - id: a
                    label: B
                    type: COMMAND
                    command: bar
                """;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(
                        new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> EscMenuConfigLoader.parse(config));
        assertNotNull(error.getMessage());
        assertTrue(error.getMessage().contains("duplicado"));
    }

    @Test
    void commandTypeRequiresCommand() {
        String yaml = """
                pause:
                  label: Test
                hub:
                  title: Test
                command:
                  enabled: false
                  name: menu
                buttons:
                  - id: broken
                    label: X
                    type: COMMAND
                """;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(
                        new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> EscMenuConfigLoader.parse(config));
    }

    private static EscMenuButton find(List<EscMenuButton> buttons, String id) {
        for (EscMenuButton button : buttons) {
            if (button.id().equals(id)) {
                return button;
            }
        }
        throw new AssertionError("botão ausente: " + id);
    }
}