package cn.wusheng.oatz.access;

/** The hook's server-owned phase, replicated by the existing entity data channel. */
public interface FlyingHookAccess {
    byte wsee$getPhase();

    void wsee$setPhase(byte phase);

    boolean wsee$isOffhand();

    void wsee$setOffhand(boolean offhand);
}
