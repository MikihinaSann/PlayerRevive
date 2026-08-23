package team.creative.playerrevive.api;

import team.creative.playerrevive.cap.Bleeding;

/**
 * Mixin interface injected into PlayerEntity to store bleeding data.
 * Replaces NeoForge's attachment system.
 */
public interface BleedingHolder {

    Bleeding playerrevive$getBleeding();

    void playerrevive$setBleeding(Bleeding bleeding);

}
