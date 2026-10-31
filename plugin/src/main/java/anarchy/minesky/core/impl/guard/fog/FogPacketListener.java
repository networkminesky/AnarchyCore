package anarchy.minesky.core.impl.guard.fog;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.entity.Player;

public final class FogPacketListener extends PacketListenerAbstract {

    private final FogOfWarManager fog;

    public FogPacketListener(FogOfWarManager fog) {
        super(PacketListenerPriority.NORMAL);
        this.fog = fog;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player observer)) {
            return;
        }
        if (!(event.getPacketType() instanceof PacketType.Play.Server type)) {
            return;
        }
        int entityId = entityId(event, type);
        if (entityId != -1 && fog.isHidden(observer.getUniqueId(), entityId)) {
            event.setCancelled(true);
        }
    }

    private static int entityId(PacketSendEvent event, PacketType.Play.Server type) {
        if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            return new WrapperPlayServerEntityRelativeMove(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            return new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_ROTATION) {
            return new WrapperPlayServerEntityRotation(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            return new WrapperPlayServerEntityTeleport(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
            return new WrapperPlayServerEntityVelocity(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_METADATA) {
            return new WrapperPlayServerEntityMetadata(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            return new WrapperPlayServerEntityEquipment(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_HEAD_LOOK) {
            return new WrapperPlayServerEntityHeadLook(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_STATUS) {
            return new WrapperPlayServerEntityStatus(event).getEntityId();
        }
        if (type == PacketType.Play.Server.ENTITY_ANIMATION) {
            return new WrapperPlayServerEntityAnimation(event).getEntityId();
        }
        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            return new WrapperPlayServerSpawnEntity(event).getEntityId();
        }
        return -1;
    }
}
