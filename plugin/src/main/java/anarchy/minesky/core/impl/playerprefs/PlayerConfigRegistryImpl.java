package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOption;
import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOptionSpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigCategory;
import anarchy.minesky.core.api.playerconfig.PlayerConfigCategorySpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigOptionType;
import anarchy.minesky.core.api.playerconfig.PlayerConfigRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlayerConfigRegistryImpl implements PlayerConfigRegistry {

    private final PlayerPreferencesService prefsService;
    private final PlayerConfigAPIImpl api;
    private final Map<String, PlayerConfigCategoryImpl> categories = new LinkedHashMap<>();
    private final Map<String, PlayerConfigBooleanOptionImpl> optionsByKey = new LinkedHashMap<>();
    private final List<PlayerConfigBooleanOptionImpl> optionsByIndex = new CopyOnWriteArrayList<>();

    public PlayerConfigRegistryImpl(PlayerPreferencesService prefsService, PlayerConfigAPIImpl api) {
        this.prefsService = prefsService;
        this.api = api;
    }

    @Override
    public synchronized PlayerConfigCategory registerCategory(Plugin owner, PlayerConfigCategorySpec spec) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(spec, "spec");
        if (categories.containsKey(spec.id())) {
            throw new IllegalStateException("Categoria já registrada: " + spec.id());
        }
        PlayerConfigCategoryImpl category = new PlayerConfigCategoryImpl(
                spec.id(),
                spec.title(),
                spec.buttonLabel(),
                spec.iconMaterial(),
                spec.sortOrder(),
                owner);
        categories.put(spec.id(), category);
        return category;
    }

    @Override
    public synchronized PlayerConfigBooleanOption registerBooleanOption(
            Plugin owner,
            PlayerConfigBooleanOptionSpec spec) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(spec, "spec");
        PlayerConfigCategoryImpl category = categories.get(spec.categoryId());
        if (category == null) {
            throw new IllegalStateException("Categoria inexistente: " + spec.categoryId());
        }
        if (optionsByKey.containsKey(spec.key())) {
            throw new IllegalStateException("Opção já registrada: " + spec.key());
        }
        int index = optionsByIndex.size();
        PlayerConfigBooleanOptionImpl option = new PlayerConfigBooleanOptionImpl(
                api,
                index,
                spec.key(),
                spec.label(),
                spec.defaultValue(),
                category,
                owner);
        optionsByKey.put(spec.key(), option);
        optionsByIndex.add(option);
        category.addOption(option);
        prefsService.onBooleanOptionRegistered(index, spec.defaultValue());
        return option;
    }

    @Override
    public Collection<PlayerConfigCategory> categories() {
        List<PlayerConfigCategory> sorted = new ArrayList<>(categories.values());
        sorted.sort((a, b) -> {
            int order = Integer.compare(a.sortOrder(), b.sortOrder());
            if (order != 0) {
                return order;
            }
            return a.id().compareTo(b.id());
        });
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public Optional<PlayerConfigCategory> category(String id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public Optional<PlayerConfigBooleanOption> booleanOption(String key) {
        return Optional.ofNullable(optionsByKey.get(key));
    }

    List<PlayerConfigBooleanOptionImpl> optionsByIndex() {
        return optionsByIndex;
    }

    PlayerConfigBooleanOptionImpl optionAt(int index) {
        if (index < 0 || index >= optionsByIndex.size()) {
            return null;
        }
        return optionsByIndex.get(index);
    }

    boolean defaultAt(int index) {
        PlayerConfigBooleanOptionImpl option = optionAt(index);
        return option != null && option.defaultValue();
    }

    static final class PlayerConfigCategoryImpl implements PlayerConfigCategory {

        private final String id;
        private final Component title;
        private final Component buttonLabel;
        private final Material iconMaterial;
        private final int sortOrder;
        private final Plugin owner;
        private final List<PlayerConfigBooleanOption> options = new ArrayList<>();

        PlayerConfigCategoryImpl(
                String id,
                Component title,
                Component buttonLabel,
                Material iconMaterial,
                int sortOrder,
                Plugin owner) {
            this.id = id;
            this.title = title;
            this.buttonLabel = buttonLabel;
            this.iconMaterial = iconMaterial;
            this.sortOrder = sortOrder;
            this.owner = owner;
        }

        void addOption(PlayerConfigBooleanOption option) {
            options.add(option);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Component title() {
            return title;
        }

        @Override
        public Component buttonLabel() {
            return buttonLabel;
        }

        @Override
        public Material iconMaterial() {
            return iconMaterial;
        }

        @Override
        public int sortOrder() {
            return sortOrder;
        }

        @Override
        public Plugin owner() {
            return owner;
        }

        @Override
        public List<PlayerConfigBooleanOption> booleanOptions() {
            return Collections.unmodifiableList(options);
        }
    }

    static final class PlayerConfigBooleanOptionImpl implements PlayerConfigBooleanOption {

        private final PlayerConfigAPIImpl api;
        private final int index;
        private final String key;
        private final Component label;
        private final boolean defaultValue;
        private final PlayerConfigCategory category;
        private final Plugin owner;

        PlayerConfigBooleanOptionImpl(
                PlayerConfigAPIImpl api,
                int index,
                String key,
                Component label,
                boolean defaultValue,
                PlayerConfigCategory category,
                Plugin owner) {
            this.api = api;
            this.index = index;
            this.key = key;
            this.label = label;
            this.defaultValue = defaultValue;
            this.category = category;
            this.owner = owner;
        }

        int index() {
            return index;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public Component label() {
            return label;
        }

        @Override
        public boolean defaultValue() {
            return defaultValue;
        }

        @Override
        public PlayerConfigCategory category() {
            return category;
        }

        @Override
        public PlayerConfigOptionType type() {
            return PlayerConfigOptionType.BOOLEAN;
        }

        @Override
        public boolean get(org.bukkit.entity.Player player) {
            return api.getBoolean(this, player);
        }

        @Override
        public boolean get(java.util.UUID playerId) {
            return api.getBoolean(this, playerId);
        }
    }
}
