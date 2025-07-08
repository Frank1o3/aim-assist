package aimmod.assist;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public enum GhostRenderer {
    INSTANCE;

    private Vec3d ghostPos = null;

    /* Glass looks “ghost‑y”, feel free to swap in any BlockState. */
    private final BlockState GHOST_MODEL = Blocks.GLASS.getDefaultState();

    GhostRenderer() {
        /* AFTER_TRANSLUCENT → depth buffer already contains world & entities. */
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ghostPos == null)
                return;

            MatrixStack matrices = ctx.matrixStack();
            Vec3d cam = ctx.camera().getPos();

            matrices.push();
            matrices.translate(
                    ghostPos.x - cam.x,
                    ghostPos.y - cam.y,
                    ghostPos.z - cam.z);

            BlockRenderManager brm = MinecraftClient.getInstance().getBlockRenderManager();
            // ① use ctx.consumers(), not the “effect” consumers
            brm.renderBlockAsEntity(
                    GHOST_MODEL,
                    matrices,
                    ctx.consumers(), // ← this is the only change
                    0xF000F0, // full‑bright light
                    OverlayTexture.DEFAULT_UV);

            matrices.pop();
            // ② DO NOT call draw(); the world renderer will flush at the right moment
        });        
    }

    public void setGhostPos(Vec3d pos) {
        this.ghostPos = pos;
    }

    public void clear() {
        this.ghostPos = null;
    }
}
