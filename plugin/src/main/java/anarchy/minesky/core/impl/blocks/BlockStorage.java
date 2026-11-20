package anarchy.minesky.core.impl.blocks;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BlockStorage {

    public static final String ADMIN_PERMISSION = "anarchycore.blocks.admin";

    private final Plugin plugin;
    private final BlockSettings settings;
    private final File file;
    private final Map<String, BlockDefinition> blocks = new LinkedHashMap<>();

    public BlockStorage(Plugin plugin, BlockSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.file = new File(plugin.getDataFolder(), "blocks.yml");
    }

    public void load() {
        blocks.clear();
        if (!file.exists()) {
            saveQuietly();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("blocks");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            BlockDefinition definition = parse(id, section.getConfigurationSection(id));
            if (definition != null) {
                blocks.put(definition.id(), definition);
            }
        }
    }

    public void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (BlockDefinition block : blocks.values()) {
            String base = "blocks." + block.id();
            yaml.set(base + ".location.world", block.worldName());
            yaml.set(base + ".location.x", block.x());
            yaml.set(base + ".location.y", block.y());
            yaml.set(base + ".location.z", block.z());
            yaml.set(base + ".type", block.type().name());
            yaml.set(base + ".commands", new ArrayList<>(block.commands()));
            yaml.set(base + ".cooldown-ms", block.cooldownMs());
            BlockParticleSettings particles = block.particles();
            if (particles != null && particles.enabled()) {
                yaml.set(base + ".particles.name", particles.particle().name());
                yaml.set(base + ".particles.amount", particles.amount());
                yaml.set(base + ".particles.speed", particles.speed());
                yaml.set(base + ".particles.spread", particles.spreadX());
            }
        }
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IOException("Não foi possível criar pasta de dados do plugin");
        }
        yaml.save(file);
    }

    private BlockDefinition parse(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String normalizedId = normalizeId(id);
        if (!isValidId(normalizedId)) {
            return null;
        }
        ConfigurationSection location = section.getConfigurationSection("location");
        if (location == null) {
            return null;
        }
        String world = location.getString("world");
        if (world == null || world.isBlank()) {
            return null;
        }
        int x = location.getInt("x");
        int y = location.getInt("y");
        int z = location.getInt("z");
        BlockType type = BlockType.parse(section.getString("type", "POSITION"));
        if (type == null) {
            return null;
        }
        List<String> commands = section.getStringList("commands");
        if (commands.isEmpty()) {
            String single = section.getString("command");
            if (single != null && !single.isBlank()) {
                commands = List.of(single);
            }
        }
        long cooldown = section.getLong("cooldown-ms", settings.defaultCooldownMs());
        BlockParticleSettings particles = parseParticles(section.getConfigurationSection("particles"));
        return new BlockDefinition(normalizedId, world, x, y, z, type, commands, particles, cooldown);
    }

    private static BlockParticleSettings parseParticles(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String name = section.getString("name");
        if (name == null || name.isBlank()) {
            return null;
        }
        Particle particle = parseParticle(name);
        if (particle == null) {
            return null;
        }
        int amount = Math.max(1, section.getInt("amount", 5));
        double speed = section.getDouble("speed", 0.01);
        double spread = section.getDouble("spread", 0.5);
        double spreadX = section.getDouble("spread-x", spread);
        double spreadY = section.getDouble("spread-y", spread);
        double spreadZ = section.getDouble("spread-z", spread);
        return new BlockParticleSettings(particle, amount, speed, spreadX, spreadY, spreadZ);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void saveQuietly() {
        try {
            if (!file.exists()) {
                if (plugin.getResource("blocks.yml") != null) {
                    plugin.saveResource("blocks.yml", false);
                    load();
                    return;
                }
            }
            save();
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao salvar blocks.yml: " + e.getMessage());
        }
    }

    public void put(BlockDefinition block) {
        blocks.put(block.id(), block);
        saveQuietly();
    }

    public boolean remove(String id) {
        BlockDefinition removed = blocks.remove(normalizeId(id));
        if (removed == null) {
            return false;
        }
        saveQuietly();
        return true;
    }

    public Optional<BlockDefinition> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(blocks.get(normalizeId(id)));
    }

    public Collection<BlockDefinition> all() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public List<String> ids() {
        return List.copyOf(blocks.keySet());
    }

    public static boolean isValidId(String id) {
        if (id == null || id.isBlank() || id.length() > 32) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (!Character.isLetter(lower.charAt(0))) {
            return false;
        }
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }
}