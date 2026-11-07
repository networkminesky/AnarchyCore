package anarchy.minesky.core.impl.kits;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class KitDefinition {

    public static final int CONTENT_SLOT_COUNT = 45;

    private final String id;
    private final String displayName;
    private final Material iconMaterial;
    private final int cooldownSeconds;
    private final ItemStack[] contents;

    public KitDefinition(
            String id,
            String displayName,
            Material iconMaterial,
            int cooldownSeconds,
            ItemStack[] contents) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.contents = sanitizeContents(contents);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material iconMaterial() {
        return iconMaterial;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public ItemStack[] contents() {
        return cloneContents(contents);
    }

    public String permissionNode() {
        return "anarchycore.kit." + id;
    }

    public String bypassCooldownPermissionNode() {
        return permissionNode() + ".bypasscooldown";
    }

    public Component menuButtonLabel() {
        return DialogComponentUtils.materialIconComponent(iconMaterial)
                .append(Component.space())
                .append(DialogComponentUtils.parse(displayName));
    }

    public KitDefinition withMeta(String displayName, Material iconMaterial, int cooldownSeconds) {
        return new KitDefinition(id, displayName, iconMaterial, cooldownSeconds, contents);
    }

    public KitDefinition withContents(ItemStack[] contents) {
        return new KitDefinition(id, displayName, iconMaterial, cooldownSeconds, contents);
    }

    public KitDefinition withCooldown(int cooldownSeconds) {
        return new KitDefinition(id, displayName, iconMaterial, cooldownSeconds, contents);
    }

    public static ItemStack[] emptyContents() {
        return new ItemStack[CONTENT_SLOT_COUNT];
    }

    private static ItemStack[] sanitizeContents(ItemStack[] input) {
        ItemStack[] copy = emptyContents();
        if (input == null) {
            return copy;
        }
        int limit = Math.min(input.length, CONTENT_SLOT_COUNT);
        for (int i = 0; i < limit; i++) {
            ItemStack stack = input[i];
            if (stack != null && !stack.getType().isAir()) {
                copy[i] = stack.clone();
            }
        }
        return copy;
    }

    public static ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] copy = emptyContents();
        if (source == null) {
            return copy;
        }
        int limit = Math.min(source.length, CONTENT_SLOT_COUNT);
        for (int i = 0; i < limit; i++) {
            ItemStack stack = source[i];
            if (stack != null && !stack.getType().isAir()) {
                copy[i] = stack.clone();
            }
        }
        return copy;
    }
}
