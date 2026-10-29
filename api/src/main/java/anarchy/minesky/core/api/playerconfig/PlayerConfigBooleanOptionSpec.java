package anarchy.minesky.core.api.playerconfig;

import net.kyori.adventure.text.Component;

import java.util.Objects;

public record PlayerConfigBooleanOptionSpec(
        String key,
        String categoryId,
        Component label,
        boolean defaultValue
) {
    public PlayerConfigBooleanOptionSpec {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(label, "label");
        if (key.isBlank() || categoryId.isBlank()) {
            throw new IllegalArgumentException("key e categoryId são obrigatórios");
        }
    }

    public static Builder builder(String key, String categoryId) {
        return new Builder(key, categoryId);
    }

    public static final class Builder {
        private final String key;
        private final String categoryId;
        private Component label;
        private boolean defaultValue = true;

        private Builder(String key, String categoryId) {
            this.key = key;
            this.categoryId = categoryId;
        }

        public Builder label(Component label) {
            this.label = label;
            return this;
        }

        public Builder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public PlayerConfigBooleanOptionSpec build() {
            return new PlayerConfigBooleanOptionSpec(key, categoryId, label, defaultValue);
        }
    }
}
