package anarchy.minesky.core.impl.kits;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MaterialPickerDialogs;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.utils.ItemRichText;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitManager {

    public static final String ADMIN_PERMISSION = "anarchycore.kits.admin";
    private static final String INPUT_DISPLAY = "display";
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 3_600;
    private static final int SIX_HOURS = 21_600;

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final KitStorage storage;
    private final KitCooldownStorage cooldowns;

    private final Map<UUID, Material> editorIconSelection = new ConcurrentHashMap<>();
    private final Map<UUID, AdminEditState> adminEditStates = new ConcurrentHashMap<>();

    public KitManager(
            MessagesManager messages,
            FeedbackManager feedback,
            KitStorage storage,
            KitCooldownStorage cooldowns) {
        this.messages = messages;
        this.feedback = feedback;
        this.storage = storage;
        this.cooldowns = cooldowns;
    }

    public KitStorage storage() {
        return storage;
    }

    public void reloadStorage() {
        storage.load();
    }

    public void handleQuit(UUID playerId) {
        editorIconSelection.remove(playerId);
        adminEditStates.remove(playerId);
        cooldowns.unload(playerId);
    }

    public void preloadPlayer(UUID playerId) {
        cooldowns.preload(playerId);
    }

    public void handleMenuClose(Player player, KitInventoryHolder holder) {
        if (holder.menuType() != KitInventoryHolder.MenuType.ADMIN_CONTENT) {
            return;
        }
        adminEditStates.remove(player.getUniqueId());
    }

    public void openKitList(Player player) {
        List<KitDefinition> kits = storage.all().stream().toList();
        if (kits.isEmpty()) {
            notify(player, messages.component("kits.error.none"), true);
            return;
        }

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.raw("kits.dialog.list.title"))
                .bodyItemIcon(new ItemStack(Material.CHEST))
                .multiActionColumns(1);

        for (KitDefinition kit : kits) {
            builder.button(DialogButton.builder()
                    .label(kit.menuButtonLabel())
                    .width(250)
                    .onClick(p -> openPlayerPreview(p, kit.id()))
                    .build());
        }

        builder.show(player);
    }

    public boolean removeKit(String kitId) {
        return storage.remove(KitStorage.normalizeId(kitId));
    }

    public void beginCreateKit(Player player, String kitId) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            notify(player, messages.component("commands.no-permission"), true);
            return;
        }
        if (!KitStorage.isValidId(kitId)) {
            notify(player, messages.component("kits.error.invalid-id"), true);
            return;
        }
        String id = KitStorage.normalizeId(kitId);
        if (storage.get(id).isPresent()) {
            notify(player, messages.component("kits.error.already-exists", "kit", id), true);
            return;
        }
        openEditorDialog(player, id, null, Material.CHEST, 0, true);
    }

    public void beginEditKit(Player player, String kitId) {
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            notify(player, messages.component("commands.no-permission"), true);
            return;
        }
        if (!KitStorage.isValidId(kitId)) {
            notify(player, messages.component("kits.error.invalid-id"), true);
            return;
        }
        String id = KitStorage.normalizeId(kitId);
        KitDefinition existing = storage.get(id).orElse(null);
        if (existing == null) {
            notify(player, messages.component("kits.error.not-found", "kit", id), true);
            return;
        }
        openEditorDialog(player, id, existing.displayName(), existing.iconMaterial(), existing.cooldownSeconds(), false);
    }

    private void openEditorDialog(
            Player player,
            String id,
            String initialDisplay,
            Material initialIcon,
            int initialCooldown,
            boolean create) {
        String titleKey = create ? "kits.dialog.editor.create-title" : "kits.dialog.editor.edit-title";
        String bodyKey = create ? "kits.dialog.editor.create-body" : "kits.dialog.editor.edit-body";

        Material selectedIcon = editorIconSelection.getOrDefault(player.getUniqueId(), initialIcon);
        Component body = messages.component(bodyKey, "id", id)
                .append(Component.newline())
                .append(DialogComponentUtils.materialIconComponent(selectedIcon))
                .append(Component.space())
                .append(Component.text(selectedIcon.name()));

        MineSkyDialog.builder(messages.raw(titleKey))
                .body(body)
                .addInput(TextInput.builder(INPUT_DISPLAY, messages.raw("kits.dialog.editor.input-display"))
                        .placeholder(messages.raw("kits.dialog.editor.input-display-placeholder"))
                        .initial(initialDisplay != null ? initialDisplay : id)
                        .maxLength(64)
                        .build())
                .button(DialogButton.builder()
                        .label(messages.raw("kits.dialog.editor.pick-icon-button"))
                        .width(220)
                        .onClick((editor, response) -> {
                            String displayDraft = response.getText(INPUT_DISPLAY, id).trim();
                            if (displayDraft.isEmpty()) {
                                displayDraft = id;
                            }
                            String finalDisplay = displayDraft;
                            MaterialPickerDialogs.open(editor, messages.raw("kits.dialog.editor.pick-icon-title"),
                                    material -> {
                                        if (material == null || material.isAir()) {
                                            return;
                                        }
                                        editorIconSelection.put(editor.getUniqueId(), material);
                                        openEditorDialog(editor, id, finalDisplay, material, initialCooldown, create);
                                    });
                        })
                        .build())
                .button(messages.raw("kits.dialog.editor.save-button"), (editor, response) -> {
                    String display = response.getText(INPUT_DISPLAY, id).trim();
                    if (display.isEmpty()) {
                        display = id;
                    }
                    Material icon = editorIconSelection.getOrDefault(editor.getUniqueId(), selectedIcon);
                    editorIconSelection.remove(editor.getUniqueId());

                    KitDefinition previous = storage.get(id).orElse(null);
                    int cooldown = previous != null ? previous.cooldownSeconds() : Math.max(0, initialCooldown);
                    ItemStack[] contents = previous != null ? previous.contents() : KitDefinition.emptyContents();
                    KitDefinition kit = new KitDefinition(id, display, icon, cooldown, contents);
                    storage.put(kit);
                    notify(editor, messages.component("kits.meta-saved", "kit", display), false);
                    openAdminContentEditor(editor, id);
                })
                .button(messages.raw("kits.dialog.editor.cancel-button"), p -> {
                    editorIconSelection.remove(p.getUniqueId());
                    notify(p, messages.component("kits.dialog.editor.cancelled"), false);
                })
                .show(player);
    }

    public void openAdminContentEditor(Player player, String kitId) {
        String id = KitStorage.normalizeId(kitId);
        KitDefinition kit = storage.get(id).orElse(null);
        if (kit == null) {
            notify(player, messages.component("kits.error.not-found", "kit", id), true);
            return;
        }

        adminEditStates.put(player.getUniqueId(), new AdminEditState(id, kit.cooldownSeconds()));

        KitInventoryHolder holder = new KitInventoryHolder(KitInventoryHolder.MenuType.ADMIN_CONTENT, id);
        Component title = messages.component("kits.menu.admin.title", "kit", kit.displayName());
        Inventory inventory = Bukkit.createInventory(holder, KitUiSlots.INVENTORY_SIZE, title);
        holder.attach(inventory);

        ItemStack[] contents = kit.contents();
        for (int i = 0; i < KitUiSlots.CONTENT_SLOTS; i++) {
            ItemStack stack = contents[i];
            if (stack != null && !stack.getType().isAir()) {
                inventory.setItem(i, stack.clone());
            }
        }

        fillAdminControlRow(inventory, kit.cooldownSeconds());
        player.openInventory(inventory);
    }

    public void openPlayerPreview(Player player, String kitId) {
        String id = KitStorage.normalizeId(kitId);
        KitDefinition kit = storage.get(id).orElse(null);
        if (kit == null) {
            notify(player, messages.component("kits.error.not-found", "kit", id), true);
            return;
        }

        KitInventoryHolder holder = new KitInventoryHolder(KitInventoryHolder.MenuType.PLAYER_PREVIEW, id);
        Component title = messages.component("kits.menu.preview.title", "kit", kit.displayName());
        Inventory inventory = Bukkit.createInventory(holder, KitUiSlots.INVENTORY_SIZE, title);
        holder.attach(inventory);

        ItemStack[] contents = kit.contents();
        for (int i = 0; i < KitUiSlots.CONTENT_SLOTS; i++) {
            ItemStack stack = contents[i];
            if (stack != null && !stack.getType().isAir()) {
                inventory.setItem(i, stack.clone());
            }
        }

        fillPreviewControlRow(inventory);
        refreshClaimButton(player, inventory, kit);
        player.openInventory(inventory);
    }

    public void handleAdminControlClick(Player player, String kitId, int slot) {
        String id = KitStorage.normalizeId(kitId);
        AdminEditState state = adminEditStates.get(player.getUniqueId());
        if (state == null || !state.kitId().equals(id)) {
            return;
        }
        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (slot == KitUiSlots.ACTION_SAVE) {
            saveAdminContent(player, id, inventory, state.cooldownSeconds());
            return;
        }

        int delta = cooldownDeltaForSlot(slot);
        if (delta == 0) {
            return;
        }
        state.setCooldown(Math.max(0, state.cooldownSeconds() + delta));
        fillAdminControlRow(inventory, state.cooldownSeconds());
        notify(player, messages.component("kits.cooldown-updated", "time", formatDuration(state.cooldownSeconds())), false);
    }

    public void handlePreviewClaimClick(Player player, String kitId) {
        String id = KitStorage.normalizeId(kitId);
        claimKit(player, id, true);
    }

    private void saveAdminContent(Player player, String kitId, Inventory inventory, int cooldownSeconds) {
        KitDefinition existing = storage.get(kitId).orElse(null);
        if (existing == null) {
            notify(player, messages.component("kits.error.not-found", "kit", kitId), true);
            return;
        }

        ItemStack[] contents = KitDefinition.emptyContents();
        for (int i = 0; i < KitUiSlots.CONTENT_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && !stack.getType().isAir()) {
                contents[i] = stack.clone();
            }
        }

        storage.put(existing.withContents(contents).withCooldown(cooldownSeconds));
        adminEditStates.remove(player.getUniqueId());
        player.closeInventory();
        notify(player, messages.component("kits.content-saved", "kit", existing.displayName()), false);
    }

    public void claimKit(Player player, String kitId, boolean closeMenu) {
        String id = KitStorage.normalizeId(kitId);
        KitDefinition kit = storage.get(id).orElse(null);
        if (kit == null) {
            notify(player, messages.component("kits.error.not-found", "kit", id), true);
            return;
        }
        if (!player.hasPermission(kit.permissionNode())) {
            notify(player, messages.component("kits.error.no-permission", "kit", id), true);
            return;
        }

        boolean bypassCooldown = player.hasPermission(kit.bypassCooldownPermissionNode());
        if (!bypassCooldown) {
            int remaining = cooldowns.secondsRemaining(player.getUniqueId(), id, kit.cooldownSeconds());
            if (remaining > 0) {
                notify(player, messages.component("kits.error.cooldown", "time", formatDuration(remaining)), true);
                return;
            }
        }

        if (countKitStacks(kit) == 0) {
            notify(player, messages.component("kits.error.empty"), true);
            return;
        }

        KitInventorySpace.SpaceCheck space = KitInventorySpace.check(player, kit.contents());
        if (!space.fits()) {
            notify(player, messages.component("kits.error.inventory-space", "slots", String.valueOf(space.slotsShort())), true);
            return;
        }

        giveContents(player, kit);

        if (!bypassCooldown && kit.cooldownSeconds() > 0) {
            cooldowns.recordClaim(player.getUniqueId(), id);
        }

        if (closeMenu) {
            player.closeInventory();
        }
        notify(player, messages.component("kits.claim-success", "kit", kit.displayName()), false);
    }

    private static int countKitStacks(KitDefinition kit) {
        int count = 0;
        for (ItemStack stack : kit.contents()) {
            if (stack != null && !stack.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    private void giveContents(Player player, KitDefinition kit) {
        for (ItemStack stack : kit.contents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            player.getInventory().addItem(stack.clone());
        }
    }

    private void fillPreviewControlRow(Inventory inventory) {
        for (int slot = KitUiSlots.CONTENT_SLOTS; slot < KitUiSlots.INVENTORY_SIZE; slot++) {
            if (slot == KitUiSlots.ACTION_PRIMARY) {
                continue;
            }
            inventory.setItem(slot, fillerItem());
        }
    }

    private void fillAdminControlRow(Inventory inventory, int cooldownSeconds) {
        for (int slot = KitUiSlots.CONTENT_SLOTS; slot < KitUiSlots.INVENTORY_SIZE; slot++) {
            if (KitUiSlots.isAdminControlSlot(slot)) {
                continue;
            }
            inventory.setItem(slot, fillerItem());
        }

        inventory.setItem(KitUiSlots.COOLDOWN_MINUS_6H, controlItem(
                Material.RED_STAINED_GLASS_PANE, messages.raw("kits.menu.admin.cooldown-minus-6h")));
        inventory.setItem(KitUiSlots.COOLDOWN_MINUS_1H, controlItem(
                Material.RED_STAINED_GLASS_PANE, messages.raw("kits.menu.admin.cooldown-minus-1h")));
        inventory.setItem(KitUiSlots.COOLDOWN_MINUS_1M, controlItem(
                Material.RED_STAINED_GLASS_PANE, messages.raw("kits.menu.admin.cooldown-minus-1m")));
        inventory.setItem(KitUiSlots.COOLDOWN_INFO, controlItem(
                Material.CLOCK,
                messages.format("kits.menu.admin.cooldown-info", "time", formatDuration(cooldownSeconds))));
        inventory.setItem(KitUiSlots.COOLDOWN_PLUS_1M, controlItem(
                Material.LIME_STAINED_GLASS_PANE, messages.raw("kits.menu.admin.cooldown-plus-1m")));
        inventory.setItem(KitUiSlots.COOLDOWN_PLUS_1H, controlItem(
                Material.LIME_STAINED_GLASS_PANE, messages.raw("kits.menu.admin.cooldown-plus-1h")));
        inventory.setItem(KitUiSlots.COOLDOWN_PLUS_6H, controlItem(
                Material.LIME_STAINED_GLASS_PANE, messages.raw("kits.menu.admin.cooldown-plus-6h")));
        inventory.setItem(KitUiSlots.ACTION_SAVE, controlItem(
                Material.EMERALD_BLOCK, messages.raw("kits.menu.admin.save")));
    }

    private void refreshClaimButton(Player player, Inventory inventory, KitDefinition kit) {
        boolean hasPerm = player.hasPermission(kit.permissionNode());
        boolean bypassCooldown = player.hasPermission(kit.bypassCooldownPermissionNode());
        int remaining = bypassCooldown
                ? 0
                : cooldowns.secondsRemaining(player.getUniqueId(), kit.id(), kit.cooldownSeconds());

        Material icon = Material.LIME_DYE;
        String labelKey = "kits.menu.preview.claim";
        if (!hasPerm) {
            icon = Material.BARRIER;
            labelKey = "kits.menu.preview.no-permission";
        } else if (remaining > 0) {
            icon = Material.CLOCK;
            labelKey = "kits.menu.preview.cooldown";
        }

        ItemStack button = controlItem(icon, messages.format(
                labelKey,
                "time",
                formatDuration(remaining)));
        inventory.setItem(KitUiSlots.ACTION_PRIMARY, button);
    }

    private static int cooldownDeltaForSlot(int slot) {
        if (slot == KitUiSlots.COOLDOWN_MINUS_6H) {
            return -SIX_HOURS;
        }
        if (slot == KitUiSlots.COOLDOWN_MINUS_1H) {
            return -ONE_HOUR;
        }
        if (slot == KitUiSlots.COOLDOWN_MINUS_1M) {
            return -ONE_MINUTE;
        }
        if (slot == KitUiSlots.COOLDOWN_PLUS_1M) {
            return ONE_MINUTE;
        }
        if (slot == KitUiSlots.COOLDOWN_PLUS_1H) {
            return ONE_HOUR;
        }
        if (slot == KitUiSlots.COOLDOWN_PLUS_6H) {
            return SIX_HOURS;
        }
        return 0;
    }

    private ItemStack fillerItem() {
        return controlItem(Material.GRAY_STAINED_GLASS_PANE, messages.raw("kits.menu.filler"));
    }

    private ItemStack controlItem(Material material, String legacyLabel) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ItemRichText.parse(legacyLabel));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void notify(Player player, Component message, boolean error) {
        feedback.send(
                player,
                message,
                error ? FeedbackPriority.HIGH : FeedbackPriority.NORMAL,
                true,
                error ? UiSoundMode.ERROR : UiSoundMode.SUCCESS);
    }

    static String formatDuration(int seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append('d').append(' ');
        }
        if (hours > 0) {
            builder.append(hours).append('h').append(' ');
        }
        if (minutes > 0) {
            builder.append(minutes).append('m').append(' ');
        }
        if (secs > 0 || builder.isEmpty()) {
            builder.append(secs).append('s');
        }
        return builder.toString().trim();
    }

    private static final class AdminEditState {
        private final String kitId;
        private int cooldownSeconds;

        AdminEditState(String kitId, int cooldownSeconds) {
            this.kitId = kitId;
            this.cooldownSeconds = Math.max(0, cooldownSeconds);
        }

        String kitId() {
            return kitId;
        }

        int cooldownSeconds() {
            return cooldownSeconds;
        }

        void setCooldown(int cooldownSeconds) {
            this.cooldownSeconds = Math.max(0, cooldownSeconds);
        }
    }
}
