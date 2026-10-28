package anarchy.minesky.core.impl.dialogapi.renderers;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogInputField;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.BoolInput;
import anarchy.minesky.core.api.dialogapi.inputs.DropdownInput;
import anarchy.minesky.core.api.dialogapi.inputs.SliderInput;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.impl.dialogapi.responses.JavaDialogResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class JavaDialogRenderer {

    private JavaDialogRenderer() {}

    public static void renderAndShow(Plugin plugin, Player player, MineSkyDialog dialog) {
        if (plugin == null || !plugin.isEnabled() || player == null || !player.isOnline() || dialog == null) {
            return;
        }

        List<DialogBody> bodies = new ArrayList<>();
        for (Component c : dialog.getBody()) {
            bodies.add(DialogBody.plainMessage(c));
        }
        for (ItemStack item : dialog.getItems()) {
            bodies.add(DialogBody.item(item).build());
        }

        List<DialogInput> inputs = new ArrayList<>();
        for (DialogInputField<?> input : dialog.getInputs()) {
            if (input instanceof TextInput text) {
                inputs.add(DialogInput.text(text.getKey(), text.getLabel())
                        .initial(text.getDefaultValue())
                        .maxLength(text.getMaxLength())
                        .build());
            } else if (input instanceof BoolInput bool) {
                inputs.add(DialogInput.bool(bool.getKey(), bool.getLabel())
                        .initial(bool.getDefaultValue())
                        .build());
            } else if (input instanceof SliderInput slider) {
                inputs.add(DialogInput.numberRange(slider.getKey(), slider.getLabel(), slider.getMin(), slider.getMax())
                        .step(slider.getStep())
                        .initial(slider.getDefaultValue())
                        .build());
            } else if (input instanceof DropdownInput dropdown) {
                List<SingleOptionDialogInput.OptionEntry> entries = dropdown.getOptions().stream()
                        .map(opt -> SingleOptionDialogInput.OptionEntry.create(
                                opt.id(),
                                opt.label(),
                                opt.id().equalsIgnoreCase(dropdown.getDefaultValue())
                        )).toList();
                inputs.add(DialogInput.singleOption(dropdown.getKey(), dropdown.getLabel(), entries).build());
            }
        }

        List<ActionButton> actionButtons = new ArrayList<>();
        ClickCallback.Options callbackOptions = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                .build();

        List<DialogButton> buttons = dialog.getButtons();
        if (buttons.isEmpty()) {
            buttons = List.of(DialogButton.of(Component.text("Fechar", NamedTextColor.GRAY), p -> {}));
        }

        for (DialogButton btn : buttons) {
            DialogAction action = DialogAction.customClick((view, audience) -> {
                if (!(audience instanceof Player p)) return;
                if (!plugin.isEnabled()) return;
                JavaDialogResponse response = new JavaDialogResponse(p, view);
                p.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled()) return;
                    if (dialog.getSubmitAction() != null) {
                        dialog.getSubmitAction().accept(p, response);
                    }
                    btn.click(p, response);
                }, null);
            }, callbackOptions);

            actionButtons.add(ActionButton.create(
                    btn.getLabel(),
                    btn.getTooltip(),
                    btn.getWidth(),
                    action
            ));
        }

        DialogType dialogType;
        if (actionButtons.size() == 1) {
            dialogType = DialogType.notice(actionButtons.get(0));
        } else if (actionButtons.size() == 2 && !dialog.hasInputs()) {
            dialogType = DialogType.confirmation(actionButtons.get(0), actionButtons.get(1));
        } else {
            dialogType = DialogType.multiAction(actionButtons).build();
        }

        Dialog paperDialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(dialog.getTitle())
                        .body(bodies)
                        .inputs(inputs)
                        .canCloseWithEscape(dialog.isCanCloseWithEscape())
                        .build())
                .type(dialogType)
        );

        player.showDialog(paperDialog);
    }
}