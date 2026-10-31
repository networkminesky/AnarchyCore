package anarchy.minesky.core.impl.guard;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class GuardPacketListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final GuardManager guardManager;

    public GuardPacketListener(Plugin plugin, GuardManager guardManager) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.guardManager = guardManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        WrapperPlayClientUpdateSign packet = new WrapperPlayClientUpdateSign(event);
        Vector3i position = packet.getBlockPosition();
        String[] lines = packet.getTextLines();

        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                guardManager.handleSignUpdate(player, position, lines);
            }
        }, null);
    }
}
