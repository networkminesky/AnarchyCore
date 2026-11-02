package anarchy.minesky.core.impl.guard.fog;

final class FogPairState {

    boolean visible = true;
    int ticksOutOfSight;
    boolean nametagSpawned;
    int floatingTagId;
    int floatGeneration;
    boolean floatPending;
    boolean locatorHidden;
    int nametagCooldown;
    double nametagX;
    double nametagY;
    double nametagZ;
    Boolean lastRayClear;
}
