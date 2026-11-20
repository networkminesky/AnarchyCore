package anarchy.minesky.core.impl.blocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public final class BlockDefinition {

    private final String id;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final BlockType type;
    private final List<String> commands;
    private final BlockParticleSettings particles;
    private final long cooldownMs;

    public BlockDefinition(
            String id,
            String worldName,
            int x,
            int y,
            int z,
            BlockType type,
            List<String> commands,
            BlockParticleSettings particles,
            long cooldownMs) {
        this.id = id;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.commands = List.copyOf(commands);
        this.particles = particles;
        this.cooldownMs = cooldownMs;
    }

    public String id() {
        return id;
    }

    public String worldName() {
        return worldName;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public BlockType type() {
        return type;
    }

    public List<String> commands() {
        return commands;
    }

    public BlockParticleSettings particles() {
        return particles;
    }

    public long cooldownMs() {
        return cooldownMs;
    }

    public String positionKey() {
        return worldName + ':' + x + ':' + y + ':' + z;
    }

    public long chunkKey() {
        return packChunk(x >> 4, z >> 4);
    }

    public Location centerLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }

    public boolean matchesPosition(String world, int blockX, int blockY, int blockZ) {
        return worldName.equals(world) && x == blockX && y == blockY && z == blockZ;
    }

    public BlockDefinition withLocation(String world, int blockX, int blockY, int blockZ) {
        return new BlockDefinition(id, world, blockX, blockY, blockZ, type, commands, particles, cooldownMs);
    }

    public BlockDefinition withType(BlockType newType) {
        return new BlockDefinition(id, worldName, x, y, z, newType, commands, particles, cooldownMs);
    }

    public BlockDefinition withCommands(List<String> newCommands) {
        return new BlockDefinition(id, worldName, x, y, z, type, newCommands, particles, cooldownMs);
    }

    public static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}