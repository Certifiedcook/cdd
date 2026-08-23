package dev.cd.diagnostics.module;

import dev.cd.diagnostics.DiagnosticsSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OreScanner {
    private static final int HORIZONTAL_RADIUS = 48;
    private static final int VERTICAL_RADIUS = 32;
    private static final int RECENTER_DISTANCE = 16;
    private static final int BLOCKS_PER_TICK = 4096;

    private static final int SIDE_XZ = HORIZONTAL_RADIUS * 2 + 1;
    private static final int SIDE_Y = VERTICAL_RADIUS * 2 + 1;
    private static final int LAYER_SIZE = SIDE_XZ * SIDE_XZ;
    private static final int TOTAL_POSITIONS = LAYER_SIZE * SIDE_Y;

    private static final Map<Long, OreHit> HITS = new HashMap<>();
    private static volatile List<OreHit> snapshot = List.of();

    private static ClientLevel level;
    private static int originX;
    private static int originY;
    private static int originZ;
    private static int cursor;
    private static boolean initialized;

    private OreScanner() {
    }

    public static void tick(Minecraft client) {
        if (!DiagnosticsSettings.oreEsp || !DiagnosticsSettings.anyOreEnabled() || client.level == null || client.player == null) {
            clear();
            return;
        }

        ClientLevel currentLevel = client.level;
        Vec3 camera = client.gameRenderer.mainCamera().position();
        int cameraX = (int) Math.floor(camera.x);
        int cameraY = (int) Math.floor(camera.y);
        int cameraZ = (int) Math.floor(camera.z);

        if (!initialized
                || currentLevel != level
                || Math.abs(cameraX - originX) >= RECENTER_DISTANCE
                || Math.abs(cameraY - originY) >= RECENTER_DISTANCE
                || Math.abs(cameraZ - originZ) >= RECENTER_DISTANCE) {
            recenter(currentLevel, cameraX, cameraY, cameraZ);
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minY = currentLevel.getMinY();
        int maxY = currentLevel.getMaxY();

        for (int scanned = 0; scanned < BLOCKS_PER_TICK; scanned++) {
            int index = cursor++;
            if (cursor >= TOTAL_POSITIONS) cursor = 0;

            int localY = index / LAYER_SIZE;
            int withinLayer = index % LAYER_SIZE;
            int localZ = withinLayer / SIDE_XZ;
            int localX = withinLayer % SIDE_XZ;

            int x = originX + localX - HORIZONTAL_RADIUS;
            int y = originY + localY - VERTICAL_RADIUS;
            int z = originZ + localZ - HORIZONTAL_RADIUS;

            if (y < minY || y >= maxY) continue;

            mutable.set(x, y, z);
            long key = mutable.asLong();
            OreType type = classify(currentLevel.getBlockState(mutable));

            if (type == null) {
                HITS.remove(key);
            } else {
                HITS.put(key, new OreHit(mutable.immutable(), type));
            }
        }

        snapshot = List.copyOf(HITS.values());
    }

    public static List<OreHit> snapshot() {
        return snapshot;
    }

    public static boolean enabled(OreType type) {
        return switch (type) {
            case DIAMOND -> DiagnosticsSettings.showDiamond;
            case ANCIENT_DEBRIS -> DiagnosticsSettings.showAncientDebris;
            case IRON -> DiagnosticsSettings.showIron;
            case GOLD -> DiagnosticsSettings.showGold;
            case LAPIS -> DiagnosticsSettings.showLapis;
            case REDSTONE -> DiagnosticsSettings.showRedstone;
        };
    }

    private static void recenter(ClientLevel currentLevel, int x, int y, int z) {
        level = currentLevel;
        originX = x;
        originY = y;
        originZ = z;
        cursor = 0;
        initialized = true;
        HITS.clear();
        snapshot = List.of();
    }

    private static void clear() {
        if (!initialized && HITS.isEmpty()) return;
        initialized = false;
        level = null;
        cursor = 0;
        HITS.clear();
        snapshot = List.of();
    }

    private static OreType classify(BlockState state) {
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            return OreType.DIAMOND;
        }
        if (state.is(Blocks.ANCIENT_DEBRIS)) {
            return OreType.ANCIENT_DEBRIS;
        }
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
            return OreType.IRON;
        }
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) {
            return OreType.GOLD;
        }
        if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) {
            return OreType.LAPIS;
        }
        if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) {
            return OreType.REDSTONE;
        }
        return null;
    }

    public enum OreType {
        DIAMOND(0xFF55FFFF),
        ANCIENT_DEBRIS(0xFFB46F52),
        IRON(0xFFE1D3B5),
        GOLD(0xFFFFD700),
        LAPIS(0xFF4169E1),
        REDSTONE(0xFFFF3838);

        private final int color;

        OreType(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }
    }

    public record OreHit(BlockPos pos, OreType type) {
    }
}
