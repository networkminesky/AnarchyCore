package anarchy.minesky.core.impl.stats;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Materiais seguros para {@link PlayerStatsSeed} (sem LEGACY_* na thread da região). */
public final class PlayerStatsMaterialIndex {

    private static volatile List<Material> mineBlockMaterials;
    private static volatile List<Material> useBlockMaterials;

    private PlayerStatsMaterialIndex() {}

    /** Aquece listas fora da thread da entidade (ex.: {@code AsyncScheduler} no startup). */
    public static void warmup() {
        mineBlockMaterials();
        useBlockMaterials();
    }

    public static List<Material> mineBlockMaterials() {
        List<Material> cached = mineBlockMaterials;
        if (cached != null) {
            return cached;
        }
        synchronized (PlayerStatsMaterialIndex.class) {
            if (mineBlockMaterials == null) {
                mineBlockMaterials = buildMineList();
            }
            return mineBlockMaterials;
        }
    }

    public static List<Material> useBlockMaterials() {
        List<Material> cached = useBlockMaterials;
        if (cached != null) {
            return cached;
        }
        synchronized (PlayerStatsMaterialIndex.class) {
            if (useBlockMaterials == null) {
                useBlockMaterials = buildUseList();
            }
            return useBlockMaterials;
        }
    }

    private static List<Material> buildMineList() {
        List<Material> out = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            if (!material.isBlock()) {
                continue;
            }
            out.add(material);
        }
        return List.copyOf(out);
    }

    private static List<Material> buildUseList() {
        List<Material> out = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            if (!material.isBlock() || !material.isItem()) {
                continue;
            }
            out.add(material);
        }
        return List.copyOf(out);
    }
}