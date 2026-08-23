package dev.cd.diagnostics.module;

import dev.cd.diagnostics.notification.CDDNotifications;
import dev.cd.diagnostics.session.CDDSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class WalkingModule {
    private static final int MAX_EXPANSIONS = 14_000;
    private static final int REPLAN_DELAY_TICKS = 12;
    private static final int STUCK_REPLAN_TICKS = 60;
    private static final int MAX_VERTICAL_TARGET_OFFSET = 3;
    private static final double WAYPOINT_REACHED_SQ = 0.22 * 0.22;

    private static BlockPos destination;
    private static List<BlockPos> path = List.of();
    private static int pathIndex;
    private static boolean active;
    private static boolean exactPath;
    private static boolean moveForward;
    private static boolean jump;
    private static int replanCooldown;
    private static int stuckTicks;
    private static double bestWaypointDistance = Double.POSITIVE_INFINITY;
    private static ClientLevel level;

    private WalkingModule() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean shouldMoveForward() {
        return active && moveForward;
    }

    public static boolean shouldJump() {
        return active && jump;
    }

    public static BlockPos destination() {
        return destination;
    }

    public static void start(Minecraft client, BlockPos target) {
        if (client.player == null || client.level == null) {
            CDDNotifications.show("Walk", "Join a world/server first");
            return;
        }
        if (CDDSession.isPanicActive()) {
            CDDNotifications.show("Walk", "Clear Panic before starting navigation");
            return;
        }
        if (FreecamModule.isActive()) {
            CDDNotifications.show("Walk", "Disable Freecam before starting navigation");
            return;
        }

        destination = target.immutable();
        level = client.level;
        active = true;
        path = List.of();
        pathIndex = 0;
        exactPath = false;
        moveForward = false;
        jump = false;
        replanCooldown = 0;
        stuckTicks = 0;
        bestWaypointDistance = Double.POSITIVE_INFINITY;

        if (!plan(client, true)) {
            stop(false);
            CDDNotifications.show("Walk", "No safe loaded path toward target");
        }
    }

    public static void stop(boolean notify) {
        boolean wasActive = active;
        active = false;
        destination = null;
        path = List.of();
        pathIndex = 0;
        exactPath = false;
        moveForward = false;
        jump = false;
        replanCooldown = 0;
        stuckTicks = 0;
        bestWaypointDistance = Double.POSITIVE_INFINITY;
        level = null;
        if (notify && wasActive) CDDNotifications.show("Walk", "Navigation stopped");
    }

    public static void tick(Minecraft client) {
        if (!active) return;
        if (client.player == null || client.level == null || client.level != level) {
            stop(false);
            return;
        }
        if (CDDSession.isPanicActive() || FreecamModule.isActive()) {
            stop(false);
            return;
        }

        if (client.gui.screen() != null) {
            moveForward = false;
            jump = false;
            return;
        }

        LocalPlayer player = client.player;

        if (replanCooldown > 0) replanCooldown--;

        while (pathIndex < path.size() && reached(player, path.get(pathIndex))) {
            pathIndex++;
            stuckTicks = 0;
            bestWaypointDistance = Double.POSITIVE_INFINITY;
        }

        if (pathIndex >= path.size()) {
            moveForward = false;
            jump = false;

            if (exactPath && destination != null && closeToDestination(player, destination)) {
                BlockPos arrived = destination;
                stop(false);
                CDDNotifications.show("Walk", "Arrived at " + arrived.getX() + " " + arrived.getY() + " " + arrived.getZ());
                return;
            }

            if (replanCooldown == 0) {
                boolean replanned = plan(client, false);
                replanCooldown = REPLAN_DELAY_TICKS;
                if (!replanned) {
                    stop(false);
                    CDDNotifications.show("Walk", "Path ended; no further safe loaded route");
                }
            }
            return;
        }

        BlockPos waypoint = path.get(pathIndex);
        double targetX = waypoint.getX() + 0.5;
        double targetZ = waypoint.getZ() + 0.5;
        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double horizontalSq = dx * dx + dz * dz;

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float delta = wrapDegrees(targetYaw - player.getYRot());
        float turn = clamp(delta, -32.0F, 32.0F);
        player.setYRot(player.getYRot() + turn);

        moveForward = Math.abs(delta) <= 70.0F;
        jump = waypoint.getY() > player.getY() + 0.45;

        double distance = Math.sqrt(horizontalSq) + Math.abs(waypoint.getY() - player.getY()) * 0.25;
        if (distance + 0.03 < bestWaypointDistance) {
            bestWaypointDistance = distance;
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }

        if (stuckTicks >= STUCK_REPLAN_TICKS && replanCooldown == 0) {
            stuckTicks = 0;
            bestWaypointDistance = Double.POSITIVE_INFINITY;
            moveForward = false;
            jump = false;
            boolean replanned = plan(client, false);
            replanCooldown = REPLAN_DELAY_TICKS;
            if (!replanned) {
                stop(false);
                CDDNotifications.show("Walk", "Stopped: route is blocked");
            }
        }
    }

    private static boolean plan(Minecraft client, boolean announce) {
        if (client.player == null || client.level == null || destination == null) return false;

        ClientLevel world = client.level;
        BlockPos start = findStandableNear(world, blockAtPlayer(client.player), 2);
        if (start == null) return false;

        BlockPos exactGoal = null;
        if (isLoaded(world, destination)) {
            exactGoal = findStandableNear(world, destination, MAX_VERTICAL_TARGET_OFFSET);
        }

        Plan result = aStar(world, start, exactGoal, destination);
        if (result == null || result.nodes().isEmpty()) return false;

        path = result.nodes();
        pathIndex = 0;
        exactPath = result.exact();
        stuckTicks = 0;
        bestWaypointDistance = Double.POSITIVE_INFINITY;

        if (announce) {
            String suffix = exactPath ? " path" : " partial path; will re-plan as chunks load";
            CDDNotifications.show("Walk", path.size() + "-step" + suffix + " to "
                    + destination.getX() + " " + destination.getY() + " " + destination.getZ());
        }
        return true;
    }

    private static Plan aStar(ClientLevel world, BlockPos start, BlockPos exactGoal, BlockPos requestedGoal) {
        PriorityQueue<SearchNode> open = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::f));
        Map<BlockPos, Double> gScore = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();

        double startHeuristic = heuristic(start, requestedGoal);
        BlockPos best = start;
        double bestHeuristic = startHeuristic;

        gScore.put(start, 0.0);
        open.add(new SearchNode(start, startHeuristic));

        int expansions = 0;
        while (!open.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            SearchNode currentEntry = open.poll();
            BlockPos current = currentEntry.pos();
            double currentG = gScore.getOrDefault(current, Double.POSITIVE_INFINITY);
            if (Double.isInfinite(currentG)) continue;

            if (exactGoal != null && current.equals(exactGoal)) {
                return new Plan(reconstruct(cameFrom, start, current), true);
            }

            double currentHeuristic = heuristic(current, requestedGoal);
            if (currentHeuristic < bestHeuristic) {
                bestHeuristic = currentHeuristic;
                best = current;
            }

            for (BlockPos next : neighbors(world, current)) {
                double tentative = currentG + 1.0 + Math.abs(next.getY() - current.getY()) * 0.22;
                double existing = gScore.getOrDefault(next, Double.POSITIVE_INFINITY);
                if (tentative >= existing) continue;

                cameFrom.put(next, current);
                gScore.put(next, tentative);
                open.add(new SearchNode(next, tentative + heuristic(next, requestedGoal)));
            }
        }

        if (best.equals(start) || bestHeuristic >= startHeuristic - 0.75) return null;
        List<BlockPos> partial = reconstruct(cameFrom, start, best);
        return partial.isEmpty() ? null : new Plan(partial, false);
    }

    private static List<BlockPos> reconstruct(Map<BlockPos, BlockPos> cameFrom, BlockPos start, BlockPos end) {
        List<BlockPos> reversed = new ArrayList<>();
        BlockPos current = end;
        while (!current.equals(start)) {
            reversed.add(current);
            current = cameFrom.get(current);
            if (current == null) return List.of();
        }
        Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private static List<BlockPos> neighbors(ClientLevel world, BlockPos current) {
        List<BlockPos> result = new ArrayList<>(4);
        addNeighbor(world, result, current, 1, 0);
        addNeighbor(world, result, current, -1, 0);
        addNeighbor(world, result, current, 0, 1);
        addNeighbor(world, result, current, 0, -1);
        return result;
    }

    private static void addNeighbor(ClientLevel world, List<BlockPos> out, BlockPos current, int dx, int dz) {
        int x = current.getX() + dx;
        int z = current.getZ() + dz;

        BlockPos same = new BlockPos(x, current.getY(), z);
        if (isStandable(world, same)) {
            out.add(same);
            return;
        }

        BlockPos up = new BlockPos(x, current.getY() + 1, z);
        if (isStandable(world, up) && canStepUp(world, current, up)) {
            out.add(up);
            return;
        }

        BlockPos down = new BlockPos(x, current.getY() - 1, z);
        if (isStandable(world, down)) out.add(down);
    }

    private static boolean canStepUp(ClientLevel world, BlockPos from, BlockPos to) {
        BlockPos aboveCurrent = from.above();
        BlockPos aboveCurrent2 = from.above(2);
        return isPassable(world, aboveCurrent) && isPassable(world, aboveCurrent2) && isPassable(world, to.above());
    }

    private static BlockPos findStandableNear(ClientLevel world, BlockPos base, int verticalRadius) {
        if (isStandable(world, base)) return base;
        for (int delta = 1; delta <= verticalRadius; delta++) {
            BlockPos up = base.above(delta);
            if (isStandable(world, up)) return up;
            BlockPos down = base.below(delta);
            if (isStandable(world, down)) return down;
        }
        return null;
    }

    private static boolean isStandable(ClientLevel world, BlockPos feet) {
        if (!isLoaded(world, feet)) return false;
        if (!isPassable(world, feet) || !isPassable(world, feet.above())) return false;

        BlockPos floorPos = feet.below();
        BlockState floor = world.getBlockState(floorPos);
        if (floor.getCollisionShape(world, floorPos).isEmpty()) return false;
        if (!floor.getFluidState().isEmpty()) return false;
        return !isDangerous(world.getBlockState(feet)) && !isDangerous(floor);
    }

    private static boolean isPassable(ClientLevel world, BlockPos pos) {
        if (!isLoaded(world, pos)) return false;
        BlockState state = world.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        if (isDangerous(state)) return false;
        return state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean isDangerous(BlockState state) {
        return state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean isLoaded(ClientLevel world, BlockPos pos) {
        LevelChunk chunk = world.getChunkSource().getChunk(
                pos.getX() >> 4,
                pos.getZ() >> 4,
                ChunkStatus.FULL,
                false
        );
        return chunk != null;
    }

    private static BlockPos blockAtPlayer(LocalPlayer player) {
        return new BlockPos(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY()),
                (int) Math.floor(player.getZ())
        );
    }

    private static boolean reached(LocalPlayer player, BlockPos waypoint) {
        double dx = player.getX() - (waypoint.getX() + 0.5);
        double dz = player.getZ() - (waypoint.getZ() + 0.5);
        return dx * dx + dz * dz <= WAYPOINT_REACHED_SQ
                && Math.abs(player.getY() - waypoint.getY()) <= 1.05;
    }

    private static boolean closeToDestination(LocalPlayer player, BlockPos target) {
        double dx = player.getX() - (target.getX() + 0.5);
        double dz = player.getZ() - (target.getZ() + 0.5);
        return dx * dx + dz * dz <= 0.75 * 0.75
                && Math.abs(player.getY() - target.getY()) <= MAX_VERTICAL_TARGET_OFFSET;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX())
                + Math.abs(a.getZ() - b.getZ())
                + Math.abs(a.getY() - b.getY()) * 0.35;
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record SearchNode(BlockPos pos, double f) {
    }

    private record Plan(List<BlockPos> nodes, boolean exact) {
    }
}
