package dev.cd.diagnostics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.module.FakePlayerModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticsOverlayRenderer {
    private static final int PLAYER_COLOR = 0xFFFFD54F;
    private static final int STORAGE_COLOR = 0xFF55DDE0;
    private static final double MAX_PLAYER_DIST_SQ = 128.0 * 128.0;
    private static final double MAX_STORAGE_DIST_SQ = 96.0 * 96.0;
    private static final int STORAGE_CHUNK_RADIUS = 5;
    private static volatile FrameState frame = FrameState.EMPTY;

    private DiagnosticsOverlayRenderer() {
    }

    public static void initialize() {
        DiagnosticRenderTypes.bootstrap();
        LevelExtractionEvents.END_EXTRACTION.register(DiagnosticsOverlayRenderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(DiagnosticsOverlayRenderer::submit);
    }

    private static void extract(LevelExtractionContext context) {
        ClientLevel level = context.level();
        Vec3 camera = context.camera().position();
        List<Target> players = new ArrayList<>();
        List<Target> storage = new ArrayList<>();

        if (DiagnosticsSettings.playerEsp) {
            Player local = Minecraft.getInstance().player;
            RemotePlayer diagnosticFake = FakePlayerModule.getFakePlayer();
            boolean fakeSeen = false;

            for (Player player : level.players()) {
                if (player == local) continue;
                if (player == diagnosticFake) fakeSeen = true;
                addPlayerTarget(players, player, camera);
            }

            // Some client entity paths do not include locally-inserted
            // RemotePlayer instances in ClientLevel.players(). Explicitly add
            // the CDD dummy once so it always exercises the real overlay path.
            if (diagnosticFake != null && !fakeSeen && diagnosticFake != local && !diagnosticFake.isRemoved()) {
                addPlayerTarget(players, diagnosticFake, camera);
            }
        }

        if (DiagnosticsSettings.storageEsp) {
            int cameraChunkX = ((int) Math.floor(camera.x)) >> 4;
            int cameraChunkZ = ((int) Math.floor(camera.z)) >> 4;
            for (int dx = -STORAGE_CHUNK_RADIUS; dx <= STORAGE_CHUNK_RADIUS; dx++) {
                for (int dz = -STORAGE_CHUNK_RADIUS; dz <= STORAGE_CHUNK_RADIUS; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunk(cameraChunkX + dx, cameraChunkZ + dz, ChunkStatus.FULL, false);
                    if (chunk == null) continue;
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (!isStorage(blockEntity)) continue;
                        BlockPos pos = blockEntity.getBlockPos();
                        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (center.distanceToSqr(camera) > MAX_STORAGE_DIST_SQ) continue;
                        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0).inflate(0.025);
                        storage.add(new Target(box, center, STORAGE_COLOR));
                    }
                }
            }
        }

        frame = new FrameState(List.copyOf(players), List.copyOf(storage), camera);
    }

    private static void addPlayerTarget(List<Target> targets, Player player, Vec3 camera) {
        AABB box = player.getBoundingBox().inflate(0.025);
        Vec3 center = center(box);
        if (center.distanceToSqr(camera) <= MAX_PLAYER_DIST_SQ) {
            targets.add(new Target(box, center, PLAYER_COLOR));
        }
    }

    private static boolean isStorage(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity
                || blockEntity instanceof EnderChestBlockEntity;
    }

    private static void submit(LevelRenderContext context) {
        FrameState snapshot = frame;
        if (snapshot.players().isEmpty() && snapshot.storage().isEmpty()) return;

        // Player diagnostics are deliberately x-ray style: the no-depth
        // pipeline is always used so player boxes remain visible through
        // terrain. The depth override toggle continues to control storage.
        RenderType playerRenderType = DiagnosticRenderTypes.NO_DEPTH_LINES;
        RenderType storageRenderType = DiagnosticsSettings.depthOverride
                ? DiagnosticRenderTypes.NO_DEPTH_LINES
                : RenderTypes.lines();

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-snapshot.camera().x, -snapshot.camera().y, -snapshot.camera().z);

        for (Target target : snapshot.players()) {
            context.submitNodeCollector().submitShapeOutline(
                    poseStack,
                    Shapes.create(target.box()),
                    playerRenderType,
                    target.color(),
                    2.0F,
                    false
            );
        }

        for (Target target : snapshot.storage()) {
            context.submitNodeCollector().submitShapeOutline(
                    poseStack,
                    Shapes.create(target.box()),
                    storageRenderType,
                    target.color(),
                    2.0F,
                    false
            );
        }

        if (DiagnosticsSettings.tracers) {
            if (!snapshot.players().isEmpty()) {
                context.submitNodeCollector().submitCustomGeometry(poseStack, playerRenderType, (pose, vertices) -> {
                    for (Target target : snapshot.players()) {
                        emitLine(pose, vertices, snapshot.camera(), target.center(), target.color());
                    }
                });
            }

            if (!snapshot.storage().isEmpty()) {
                context.submitNodeCollector().submitCustomGeometry(poseStack, storageRenderType, (pose, vertices) -> {
                    for (Target target : snapshot.storage()) {
                        emitLine(pose, vertices, snapshot.camera(), target.center(), target.color());
                    }
                });
            }
        }

        poseStack.popPose();
    }

    private static void emitLine(PoseStack.Pose pose, VertexConsumer vertices, Vec3 from, Vec3 to, int color) {
        float dx = (float) (to.x - from.x);
        float dy = (float) (to.y - from.y);
        float dz = (float) (to.z - from.z);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5F) return;
        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;

        vertices.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(1.5F);
        vertices.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(1.5F);
    }

    private static Vec3 center(AABB box) {
        return new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
    }

    private record Target(AABB box, Vec3 center, int color) {
    }

    private record FrameState(List<Target> players, List<Target> storage, Vec3 camera) {
        private static final FrameState EMPTY = new FrameState(List.of(), List.of(), Vec3.ZERO);
    }
}
