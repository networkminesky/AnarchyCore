package anarchy.minesky.core.api.playerdata;

@FunctionalInterface
public interface PlayerDataValueCallback {

    void onComplete(PlayerDataResult<PlayerDataValue> result);
}
