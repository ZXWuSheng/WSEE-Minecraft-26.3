package cn.wusheng.oatz.qa;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Only loaded by the isolated movementValidation Gradle run. */
@Mod("wsee_server_validation")
public final class NeoQaMod {
    public NeoQaMod() {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> RuntimeSuite.start(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> RuntimeSuite.tick(event.getServer()));
    }
}
