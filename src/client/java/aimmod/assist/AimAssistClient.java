package aimmod.assist;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

public class AimAssistClient implements ClientModInitializer {

    public static final String MOD_ID = "aim-assist";

    private static KeyBinding toggleKey;
    private static boolean localToggle = true;
    private static ConfigHolder<AimAssistConfig> HOLDER;

    @Override
    public void onInitializeClient() {
        HOLDER = AutoConfig.register(AimAssistConfig.class, JanksonConfigSerializer::new);

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aim-assist.toggle",
                GLFW.GLFW_KEY_Z,
                "key.categories.aim-assist"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    /* ───────────────────────────────────────────────────────────────── */
    private void tick(MinecraftClient mc) {
        /* Handle toggle key */
        while (toggleKey.wasPressed()) {
            localToggle = !localToggle;
            System.out.println("[AimAssist] Toggled " + (localToggle ? "ON" : "OFF"));
        }

        GhostRenderer.INSTANCE.clear(); // clear ghost every frame

        if (mc.player == null)
            return;

        AimAssistConfig cfg = HOLDER.get();
        if (!cfg.enabled || !localToggle)
            return;

        /* ================== AIM‑ASSIST LOGIC =================== */
        LivingEntity target = selectTarget(mc, cfg);
        if (target != null) {
            float pull = BowItem.getPullProgress(mc.player.getItemUseTime());
            Vec3d aimDir = Ballistics.solve(mc.player, target, pull, cfg);
            if (aimDir != null) {
                float curYaw = mc.player.getYaw();
                float curPitch = mc.player.getPitch();
                float tgtYaw = (float) Math.toDegrees(Math.atan2(-aimDir.x, aimDir.z));
                float tgtPitch = (float) Math.toDegrees(Math.asin(-aimDir.y));
                float f = cfg.assistStrengthPercent / 100.0f;
                float desYaw = lerpDeg(curYaw, tgtYaw, f);
                float desPitch = lerpDeg(curPitch, tgtPitch, f);
                float dYaw = clampDelta(wrapDeg(desYaw - curYaw), cfg.clamp);
                float dPitch = clampDelta(wrapDeg(desPitch - curPitch), cfg.clamp);
                mc.player.setYaw(curYaw + dYaw);
                mc.player.setPitch(curPitch + dPitch);
                if (cfg.showGhostMarker) {
                    GhostRenderer.INSTANCE.setGhostPos(
                            mc.player.getEyePos().add(aimDir.normalize()));
                }
            }
        }
    }

    /* --------------------------------- helpers ----------------------------- */
    private static float wrapDeg(float deg) {
        return (deg + 540.0f) % 360.0f - 180.0f;
    }

    private static float clampDelta(float value, float limit) {
        if (limit <= 0)
            return value;
        return Math.max(-limit, Math.min(limit, value));
    }

    private float lerpDeg(float a, float b, float t) {
        float diff = ((b - a + 540) % 360) - 180;
        return a + diff * t;
    }

    private LivingEntity selectTarget(MinecraftClient mc, AimAssistConfig cfg) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1.0F);
        double maxDistSq = cfg.maxAssistDistance * cfg.maxAssistDistance;
        double bestScore = Double.MAX_VALUE;
        LivingEntity best = null;
        double fovCos = Math.cos(Math.toRadians(cfg.assistFovDegrees));
        for (LivingEntity e : mc.world.getEntitiesByClass(
                LivingEntity.class,
                mc.player.getBoundingBox().expand(cfg.maxAssistDistance),
                ent -> ent != mc.player)) {
            if (e instanceof PlayerEntity p &&
                    (p.isCreative() || p.isSpectator()))
                continue;
            Vec3d to = e.getPos().add(0, e.getStandingEyeHeight(), 0).subtract(eye);
            double distSq = to.lengthSquared();
            if (distSq > maxDistSq || distSq < 0.0001)
                continue;
            if (look.dotProduct(to.normalize()) < fovCos)
                continue;
            HitResult hr = mc.world.raycast(new RaycastContext(
                    eye, eye.add(to),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player));
            if (hr.getType() == HitResult.Type.BLOCK)
                continue;
            double angleScore = 1 - look.dotProduct(to.normalize());
            if (angleScore < bestScore) {
                bestScore = angleScore;
                best = e;
            }
        }
        return best;
    }
}