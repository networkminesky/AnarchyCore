package anarchy.minesky.core.impl.guard.fog;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/** Posição e estado lidos só na entity thread do dono; outras threads só leem estes campos. */
final class FogSnapshot {

    final UUID playerId;
    volatile UUID worldId;
    volatile String worldName = "";
    volatile int entityId;
    volatile double x;
    volatile double y;
    volatile double z;
    volatile double eyeY;
    volatile double height = 1.8;
    volatile double vx;
    volatile double vy;
    volatile double vz;
    volatile float yaw;
    volatile float pitch;
    volatile float bodyYaw;
    volatile boolean sneaking;
    volatile boolean invisible;
    volatile boolean helmetStealth;
    volatile boolean onGround;
    volatile byte skinParts = (byte) 0x7F;
    volatile boolean vanished;
    volatile Component displayName = Component.empty();
    volatile ItemStack[] equipment = new ItemStack[6];
    volatile boolean valid;

    FogSnapshot(UUID playerId) {
        this.playerId = playerId;
    }

    boolean stealthed() {
        return sneaking || invisible || helmetStealth;
    }
}
