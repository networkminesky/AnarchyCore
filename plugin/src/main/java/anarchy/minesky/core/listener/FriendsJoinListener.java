package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.friends.FriendsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FriendsJoinListener implements Listener {

    private final FriendsManager friends;

    public FriendsJoinListener(FriendsManager friends) {
        this.friends = friends;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        friends.notifyFriendsOfJoin(event.getPlayer());
    }
}
