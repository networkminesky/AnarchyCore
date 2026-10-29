package anarchy.minesky.core.api.playerconfig;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.Objects;

/**
 * Dados para registrar uma categoria no {@link PlayerConfigRegistry}.
 * {@link #iconMaterial()} é obrigatório (vira sprite no botão da categoria).
 */
public record PlayerConfigCategorySpec(
        String id,
        Component title,
        Component buttonLabel,
        Material iconMaterial,
        int sortOrder
) {
    public PlayerConfigCategorySpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(buttonLabel, "buttonLabel");
        Objects.requireNonNull(iconMaterial, "iconMaterial");
        if (iconMaterial.isAir()) {
            throw new IllegalArgumentException("iconMaterial não pode ser AIR");
        }
        if (id.isBlank()) {
            throw new IllegalArgumentException("id vazio");
        }
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private Component title;
        private Component buttonLabel;
        private Material iconMaterial;
        private int sortOrder;

        private Builder(String id) {
            this.id = id;
        }

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder buttonLabel(Component buttonLabel) {
            this.buttonLabel = buttonLabel;
            return this;
        }

        public Builder iconMaterial(Material iconMaterial) {
            this.iconMaterial = iconMaterial;
            return this;
        }

        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public PlayerConfigCategorySpec build() {
            return new PlayerConfigCategorySpec(id, title, buttonLabel, iconMaterial, sortOrder);
        }
    }
}
