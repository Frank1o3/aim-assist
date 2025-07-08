package aimmod.assist;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class Ballistics {

    /**
     * Returns a unit vector that, when multiplied by bow speed and fired,
     * lets the arrow intersect the target – taking into account
     *   • the target’s motion
     *   • the shooter’s motion (the arrow inherits the full player velocity)
     *   • gravity, drag, and user settings.
     *
     * @return null when the shot is physically impossible (out of range, etc.)
     */
    public static Vec3d solve(PlayerEntity shooter,
                              LivingEntity target,
                              float draw,
                              AimAssistConfig cfg) {

        /* -----------------------------------------------------------------
         *  Work in a reference frame where the shooter is stationary.
         *  (Vanilla adds the player’s velocity to every projectile on spawn,
         *   so subtracting it here gives us the “pure” launch problem.)
         * ----------------------------------------------------------------- */
        Vec3d shooterVel = shooter.getVelocity();

        Vec3d s = shooter.getEyePos();                                     // shooter pos
        Vec3d t = target.getPos().add(0, target.getStandingEyeHeight(), 0); // target eye

        /* Relative target velocity (lead prediction) */
        Vec3d vTarget = cfg.leadTargets
                ? target.getVelocity().subtract(shooterVel)  // target minus shooter
                : Vec3d.ZERO;

        /* Muzzle speed (blocks/tick) */
        double v0 = draw * cfg.arrowSpeedFactor;

        /* First‑guess time‑of‑flight */
        double time = s.distanceTo(t) / Math.max(0.001, v0);

        /* ─── Refine lead prediction (Newton) ───────────────────────────── */
        for (int i = 0; i < cfg.iterations; i++) {
            Vec3d future = t.add(vTarget.multiply(time));
            time = s.distanceTo(future) / Math.max(0.001, v0);
            t = future;
        }

        /* -----------------------------------------------------------------
         *  Flat (XZ) and vertical components, shooter now considered still.
         * ----------------------------------------------------------------- */
        Vec3d flat = new Vec3d(t.x - s.x, 0, t.z - s.z);
        double d = flat.length();          // horizontal distance
        double h = t.y - s.y;              // vertical difference

        /* -----------------------------------------------------------------
         *  If drop compensation is off, just shoot straight at the point.
         * ----------------------------------------------------------------- */
        if (!cfg.compensateDrop) {
            return flat.lengthSquared() < 1e-6 ? null
                    : new Vec3d(t.x - s.x, h, t.z - s.z).normalize();
        }

        /* ─── Gravity solution (quadratic) ─────────────────────────────── */
        double g  = cfg.gravity;
        double v2 = v0 * v0;
        double det = v2 * v2 - g * (g * d * d + 2 * h * v2);
        if (det < 0) return null;   // unreachable

        double theta = Math.atan((v2 - Math.sqrt(det)) / (g * d));

        /* Simple exponential drag correction for long shots */
        double dragScale = Math.pow(cfg.drag, time);

        Vec3d dir = flat.normalize()
                       .multiply(Math.cos(theta) * dragScale)
                       .add(0, Math.sin(theta), 0);

        return dir.normalize();
    }

    private Ballistics() {
    }
}
