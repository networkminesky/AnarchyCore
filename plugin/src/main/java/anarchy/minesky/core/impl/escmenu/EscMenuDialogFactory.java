package anarchy.minesky.core.impl.escmenu;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public final class EscMenuDialogFactory {

    private EscMenuDialogFactory() {}

    public static void applyDialog(
            io.papermc.paper.registry.data.dialog.DialogRegistryEntry.Builder builder,
            EscMenuDefinition definition) {
        List<ActionButton> actionButtons = buildActionButtons(definition);

        MultiActionType.Builder multi = DialogType.multiAction(actionButtons);
        multi.columns(definition.columns());
        if (definition.exitEnabled() && definition.exitLabel() != null) {
            multi.exitAction(ActionButton.builder(definition.exitLabel())
                    .width(definition.exitWidth())
                    .action(DialogAction.customClick(EscMenuKeys.customClick("_exit"), null))
                    .build());
        }

        builder.base(DialogBase.builder(definition.hubTitle())
                        .externalTitle(definition.pauseLabel())
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .build())
                .type(multi.build());
    }

    public static List<ActionButton> buildActionButtons(EscMenuDefinition definition) {
        List<ActionButton> actionButtons = new ArrayList<>(definition.buttons().size());
        for (EscMenuButton button : definition.buttons()) {
            actionButtons.add(toActionButton(button));
        }
        return actionButtons;
    }

    private static ActionButton toActionButton(EscMenuButton button) {
        DialogAction action = switch (button.type()) {
            case COMMAND -> DialogAction.staticAction(ClickEvent.runCommand(button.command()));
            case CUSTOM -> DialogAction.customClick(EscMenuKeys.customClick(button.id()), null);
        };
        return ActionButton.builder(button.label())
                .width(button.width())
                .tooltip(permissionTooltip(button.permission()))
                .action(action)
                .build();
    }

    private static Component permissionTooltip(String permission) {
        if (permission == null || permission.isBlank()) {
            return null;
        }
        return Component.text("Permissão: " + permission, NamedTextColor.GRAY);
    }
}