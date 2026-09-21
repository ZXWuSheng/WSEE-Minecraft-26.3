package cn.wusheng.oatz;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = WseeMod.MOD_ID, value = Dist.CLIENT)
public final class ClientParticleHooks {
    private ClientParticleHooks() {
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WseeMod.SWORD_WAVE_PARTICLE.get(), sprites ->
                (type, level, x, y, z, xd, yd, zd, random) ->
                        new WideSwordWaveParticle(level, x, y, z, sprites));
    }

    private static final class WideSwordWaveParticle extends SingleQuadParticle {
        private final SpriteSet sprites;

        private WideSwordWaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(0, 1));
            this.sprites = sprites;
            this.quadSize = 2.5F;
            this.lifetime = 3;
            this.gravity = 0.0F;
            this.setParticleSpeed(0.0D, 0.0D, 0.0D);
            this.setSpriteFromAge(sprites);
            this.setColor(0.92F, 0.96F, 1.0F);
            this.setAlpha(0.9F);
        }

        @Override
        public void tick() {
            super.tick();
            this.setSpriteFromAge(this.sprites);
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        @Override
        public FacingCameraMode getFacingCameraMode() {
            return FacingCameraMode.LOOKAT_Y;
        }
    }
}
