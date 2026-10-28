package anarchy.minesky.core.api.playerdata;

@FunctionalInterface
public interface PlayerDataFindCallback {

    void onComplete(PlayerDataResult<PlayerDataSnapshot> result);
}
