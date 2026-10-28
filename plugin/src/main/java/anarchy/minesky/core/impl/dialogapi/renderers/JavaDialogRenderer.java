package anarchy.minesky.core.impl.dialogapi.renderers;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import anarchy.minesky.core.api.dialogapi.DialogBodyItem;
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

        ClickCallback.Options callbackOptions = ClickCallback.Options.builder()
                .uses(1)
                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                .build();

        List<DialogBody> bodies = new ArrayList<>();
        appendBodyItems(bodies, dialog);
        for (Component c : dialog.getBody()) {
            bodies.add(DialogBody.plainMessage(c));
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

        List<DialogButton> buttons = dialog.getButtons();
        if (buttons.isEmpty()) {
            buttons = List.of(DialogButton.of(Component.text("Fechar", NamedTextColor.GRAY), p -> {}));
        }

        boolean inlineBefore = dialog.isButtonsBeforeBody() && !dialog.hasInputs() && !buttons.isEmpty();
        boolean inlineAfter = dialog.isButtonsAfterBody() && !dialog.hasInputs() && !buttons.isEmpty();
        boolean inlineButtons = inlineBefore || inlineAfter;

        if (inlineBefore) {
            bodies.add(0, DialogBody.plainMessage(buildInlineButtonRow(plugin, dialog, buttons, callbackOptions)));
        }
        if (inlineAfter) {
            for (DialogButton btn : buttons) {
                bodies.add(DialogBody.plainMessage(buildInlineButton(plugin, dialog, btn, callbackOptions)));
            }
        }

        List<ActionButton> actionButtons = new ArrayList<>();
        if (!inlineButtons) {
            for (DialogButton btn : buttons) {
                DialogAction action = DialogAction.customClick((view, audience) -> {
                    if (!(audience instanceof Player p)) {
                        return;
                    }
                    if (!plugin.isEnabled()) {
                        return;
                    }
                    JavaDialogResponse response = new JavaDialogResponse(p, view);
                    p.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled()) {
                            return;
                        }
                        if (dialog.getSubmitAction() != null) {
                            dialog.getSubmitAction().accept(p, response);
                        }
                        btn.click(p, response);
                        if (btn.isCloseOnClick()) {
                            p.closeDialog();
                        }
                    }, null);
                }, callbackOptions);

                actionButtons.add(ActionButton.create(
                        btn.getLabel(),
                        btn.getTooltip(),
                        btn.getWidth(),
                        action
                ));
            }
        }

        DialogType dialogType;
        if (inlineButtons) {
            dialogType = DialogType.notice();
        } else if (dialog.getMultiActionColumns() > 0) {
            MultiActionType.Builder multiBuilder = DialogType.multiAction(actionButtons);
            multiBuilder.columns(dialog.getMultiActionColumns());
            dialogType = multiBuilder.build();
        } else if (actionButtons.size() == 1) {
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

    private static void appendBodyItems(List<DialogBody> bodies, MineSkyDialog dialog) {
        for (DialogBodyItem spec : dialog.getBodyItems()) {
            bodies.add(DialogBody.item(spec.item())
                    .width(spec.width())
                    .height(spec.height())
                    .showDecorations(spec.showDecorations())
                    .showTooltip(spec.showTooltip())
                    .build());
        }
    }

    private static Component buildInlineButton(Plugin plugin, MineSkyDialog dialog, DialogButton btn,
                                               ClickCallback.Options callbackOptions) {
        return btn.getLabel().clickEvent(ClickEvent.callback(audience -> {
            if (!(audience instanceof Player player)) {
                return;
            }
            if (!plugin.isEnabled()) {
                return;
            }
            JavaDialogResponse response = new JavaDialogResponse(player, null);
            player.getScheduler().run(plugin, task -> {
                if (!plugin.isEnabled()) {
                    return;
                }
                if (dialog.getSubmitAction() != null) {
                    dialog.getSubmitAction().accept(player, response);
                }
                btn.click(player, response);
                if (btn.isCloseOnClick()) {
                    player.closeDialog();
                }
            }, null);
        }, callbackOptions));
    }

    private static Component buildInlineButtonRow(Plugin plugin, MineSkyDialog dialog, List<DialogButton> buttons,
                                                  ClickCallback.Options callbackOptions) {
        Component row = Component.empty();
        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) {
                row = row.append(Component.text("   "));
            }
            row = row.append(buildInlineButton(plugin, dialog, buttons.get(i), callbackOptions));
        }
        return row;
    }
}
