package anarchy.minesky.core.api.playerdata;

@FunctionalInterface
public interface PlayerDataSetCallback {

    void onComplete(PlayerDataResult<Void> result);
}
