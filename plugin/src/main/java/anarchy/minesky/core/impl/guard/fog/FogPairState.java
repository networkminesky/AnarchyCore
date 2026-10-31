package anarchy.minesky.core.impl.guard.fog;

final class FogPairState {

    boolean visible = true;
    int ticksOutOfSight;
    boolean nametagSpawned;
    boolean locatorHidden;
    int nametagCooldown;
    double nametagX;
    double nametagY;
    double nametagZ;
    Boolean lastRayClear;
}
