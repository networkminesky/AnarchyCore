package anarchy.minesky.core.impl.blocks;

import org.bukkit.Particle;

public record BlockParticleSettings(
        Particle particle,
        int amount,
        double speed,
        double spreadX,
        double spreadY,
        double spreadZ) {

    public static BlockParticleSettings none() {
        return null;
    }

    public boolean enabled() {
        return particle != null && amount > 0;
    }
}