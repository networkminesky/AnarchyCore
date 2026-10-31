package anarchy.minesky.core.impl.guard;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GuardPlayerSnapshot {

    private final ConcurrentMap<UUID, String> clientBrands = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastCheckAt = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> lastCheckResult = new ConcurrentHashMap<>();

    public void setClientBrand(UUID uuid, String brand) {
        if (brand != null && !brand.isBlank()) {
            clientBrands.put(uuid, brand);
        }
    }

    public String clientBrand(UUID uuid) {
        return clientBrands.getOrDefault(uuid, "desconhecido");
    }

    public void refreshFromPlayer(Player player) {
        if (player == null) {
            return;
        }
        try {
            String brand = player.getClientBrandName();
            if (brand != null && !brand.isBlank()) {
                clientBrands.put(player.getUniqueId(), brand);
            }
        } catch (NoSuchMethodError ignored) {
            // Paper API unavailable
        }
    }

    public void recordCheck(UUID uuid, String summary) {
        lastCheckAt.put(uuid, System.currentTimeMillis());
        lastCheckResult.put(uuid, summary);
    }

    public long lastCheckAt(UUID uuid) {
        return lastCheckAt.getOrDefault(uuid, 0L);
    }

    public String lastCheckResult(UUID uuid) {
        return lastCheckResult.getOrDefault(uuid, "—");
    }

    public void remove(UUID uuid) {
        clientBrands.remove(uuid);
        lastCheckAt.remove(uuid);
        lastCheckResult.remove(uuid);
    }
}
