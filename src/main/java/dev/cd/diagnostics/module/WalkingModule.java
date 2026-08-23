package dev.cd.diagnostics.module;

import dev.cd.diagnostics.notification.CDDNotifications;
import dev.cd.diagnostics.session.CDDSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class WalkingModule {
    private static final int MAX_EXPANSIONS = 18_000;
    private static final int REPLAN_DELAY_TICKS = 12;
    private static final int STUCK_REPLAN_TICKS = 70;
    private static final int MAX_VERTICAL_TARGET_OFFSET = 3;
    private static final int PLACE_RETRY_TICKS = 3;
    private static final double WAYPOINT_REACHED_SQ = 0.24 * 0.24;

    private static BlockPos destination;
    private static List<BlockPos> path = List.of();
    private static int pathIndex;
    private static boolean active;
    private static boolean exactPath;
    private static boolean moveForward;
    private static boolean jump;
    private static boolean sneak;
    private static int replanCooldown;
    private static int placeCooldown;
    private static int stuckTicks;
    private static double bestWaypointDistance = Double.POSITIVE_INFINITY;
    private static ClientLevel level;

    private static BlockPos breakingPos;
    private static int originalMiningSlot = -1;

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

    public static boolean shouldSneak() {
        return active && sneak;
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

        stop(false);
        destination = target.immutable();
        level = client.level;
        active = true;
        path = List.of();
        pathIndex = 0;
        exactPath = false;
        moveForward = false;
        jump = false;
        sneak = false;
        replanCooldown = 0;
        placeCooldown = 0;
        stuckTicks = 0;
        bestWaypointDistance = Double.POSITIVE_INFINITY;

        if (!plan(client, true)) {
            stop(false);
            CDDNotifications.show("Walk", "No route toward target with current loaded terrain/resources");
        }
    }

    public static void stop(boolean notify) {
        boolean wasActive = active;
        cancelMining(Minecraft.getInstance());
        active = false;
        destination = null;
        path = List.of();
        pathIndex = 0;
        exactPath = false;
        moveForward = false;
        jump = false;
        sneak = false;
        replanCooldown = 0;
        placeCooldown = 0;
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
            sneak = false;
            cancelMining(client);
            return;
        }

        LocalPlayer player = client.player;
        if (replanCooldown > 0) replanCooldown--;
        if (placeCooldown > 0) placeCooldown--;

        while (pathIndex < path.size() && reached(player, path.get(pathIndex))) {
            pathIndex++;
            stuckTicks = 0;
            bestWaypointDistance = Double.POSITIVE_INFINITY;
        }

        if (pathIndex >= path.size()) {
            haltMovement();
            cancelMining(client);

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
                    CDDNotifications.show("Walk", "Path ended; no further route is currently possible");
                }
            }
            return;
        }

        BlockPos waypoint = path.get(pathIndex);
        if (!prepareStep(client, player, waypoint)) return;

        double targetX = waypoint.getX() + 0.5;
        double targetZ = waypoint.getZ() + 0.5;
        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double horizontalSq = dx * dx + dz * dz;

        float delta = 0.0F;
        if (horizontalSq > 0.01) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            delta = wrapDegrees(targetYaw - player.getYRot());
            float turn = clamp(delta, -32.0F, 32.0F);
            player.setYRot(player.getYRot() + turn);
        }

        boolean swimming = player.isInWater() || isSwimCell(client.level, waypoint);
        moveForward = horizontalSq > 0.04 && (swimming || Math.abs(delta) <= 70.0F);

        if (swimming) {
            sneak = waypoint.getY() < player.getY() - 0.20;
            jump = !sneak && waypoint.getY() >= player.getY() - 0.15;
        } else {
            sneak = false;
            jump = waypoint.getY() > player.getY() + 0.42;
        }

        double distance = Math.sqrt(horizontalSq) + Math.abs(waypoint.getY() - player.getY()) * 0.35;
        if (distance + 0.03 < bestWaypointDistance) {
            bestWaypointDistance = distance;
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }

        if (stuckTicks >= STUCK_REPLAN_TICKS && replanCooldown == 0) {
            stuckTicks = 0;
            bestWaypointDistance = Double.POSITIVE_INFINITY;
            haltMovement();
            cancelMining(client);
            boolean replanned = plan(client, false);
            replanCooldown = REPLAN_DELAY_TICKS;
            if (!replanned) {
                stop(false);
                CDDNotifications.show("Walk", "Stopped: route is blocked");
            }
        }
    }

    private static boolean prepareStep(Minecraft client, LocalPlayer player, BlockPos waypoint) {
        ClientLevel world = client.level;
        if (world == null) return false;

        BlockPos blocker = firstBlockingBodyBlock(world, player, waypoint);
        if (blocker != null) {
            haltMovement();
            if (!isBreakable(world, player, blocker)) {
                stop(false);
                CDDNotifications.show("Walk", "Stopped: unbreakable block at " + format(blocker));
                return false;
            }
            mineBlock(client, player, blocker);
            return false;
        }

        cancelMining(client);

        if (!isSwimCell(world, waypoint) && !hasSafeSupport(world, waypoint.below())) {
            haltMovement();
            if (placeCooldown > 0) return false;
            if (!placeSupport(client, player, waypoint.below())) {
                stop(false);
                return false;
            }
            placeCooldown = PLACE_RETRY_TICKS;
            return false;
        }

        return true;
    }

    private static void mineBlock(Minecraft client, LocalPlayer player, BlockPos pos) {
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) {
            stop(false);
            CDDNotifications.show("Walk", "Cannot mine: game mode controller unavailable");
            return;
        }

        if (!pos.equals(breakingPos)) {
            cancelMining(client);
            originalMiningSlot = player.getInventory().getSelectedSlot();
            int bestSlot = bestMiningSlot(player, client.level.getBlockState(pos));
            if (bestSlot >= 0) player.getInventory().setSelectedSlot(bestSlot);
            breakingPos = pos.immutable();
            gameMode.startDestroyBlock(pos, Direction.UP);
        } else {
            gameMode.continueDestroyBlock(pos, Direction.UP);
        }
    }

    private static void cancelMining(Minecraft client) {
        if (breakingPos == null) return;

        if (client != null && client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
        }
        if (client != null && client.player != null && originalMiningSlot >= 0 && originalMiningSlot < 9) {
            client.player.getInventory().setSelectedSlot(originalMiningSlot);
        }

        breakingPos = null;
        originalMiningSlot = -1;
    }

    private static int bestMiningSlot(LocalPlayer player, BlockState state) {
        int selected = player.getInventory().getSelectedSlot();
        int best = selected;
        float bestSpeed = player.getInventory().getItem(selected).getDestroySpeed(state);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = slot;
            }
        }
        return best;
    }

    private static boolean placeSupport(Minecraft client, LocalPlayer player, BlockPos supportPos) {
        ClientLevel world = client.level;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (world == null || gameMode == null) {
            CDDNotifications.show("Walk", "Cannot place bridge block right now");
            return false;
        }

        int blockSlot = findPlaceableBlockSlot(player, world, supportPos);
        if (blockSlot < 0) {
            CDDNotifications.show("Walk", "Out of suitable full-cube blocks for placing/bridging");
            return false;
        }

        Placement placement = findPlacementAnchor(world, supportPos);
        if (placement == null) {
            CDDNotifications.show("Walk", "Cannot anchor a block at " + format(supportPos));
            return false;
        }

        int previousSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(blockSlot);

        Direction face = placement.face();
        BlockPos anchor = placement.anchor();
        Vec3 hit = new Vec3(
                anchor.getX() + 0.5 + face.getStepX() * 0.5,
                anchor.getY() + 0.5 + face.getStepY() * 0.5,
                anchor.getZ() + 0.5 + face.getStepZ() * 0.5
        );
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, anchor, false));
        player.swing(InteractionHand.MAIN_HAND);
        player.getInventory().setSelectedSlot(previousSlot);
        return true;
    }

    private static Placement findPlacementAnchor(ClientLevel world, BlockPos supportPos) {
        Direction[] preferredFaces = {
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST,
                Direction.DOWN
        };

        for (Direction face : preferredFaces) {
            BlockPos anchor = supportPos.relative(face.getOpposite());
            if (!isLoaded(world, anchor)) continue;
            if (world.getBlockEntity(anchor) != null) continue;
            BlockState state = world.getBlockState(anchor);
            if (isDangerous(state)) continue;
            if (!state.getFluidState().isEmpty()) continue;
            if (state.getCollisionShape(world, anchor).isEmpty()) continue;
            return new Placement(anchor.immutable(), face);
        }
        return null;
    }

    private static int findPlaceableBlockSlot(LocalPlayer player, ClientLevel world, BlockPos pos) {
        int bestSlot = -1;
        int bestCount = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            Block block = blockItem.getBlock();
            if (block instanceof FallingBlock) continue;

            BlockState state = block.defaultBlockState();
            if (isDangerous(state) || !state.getFluidState().isEmpty()) continue;
            if (!Block.isShapeFullBlock(state.getCollisionShape(world, pos))) continue;

            if (stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static boolean plan(Minecraft client, boolean announce) {
        if (client.player == null || client.level == null || destination == null) return false;

        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        BlockPos start = findCurrentNode(world, blockAtPlayer(player), 2);
        if (start == null) return false;

        BlockPos exactGoal = null;
        if (isLoaded(world, destination)) {
            exactGoal = findGoalNear(world, player, destination, MAX_VERTICAL_TARGET_OFFSET);
        }

        Plan result = aStar(world, player, start, exactGoal, destination);
        if (result == null || result.nodes().isEmpty()) return false;

        path = result.nodes();
        pathIndex = 0;
        exactPath = result.exact();
        stuckTicks = 0;
        bestWaypointDistance = Double.POSITIVE_INFINITY;

        if (announce) {
            String suffix = exactPath ? " route" : " partial route; will re-plan as chunks load";
            CDDNotifications.show("Walk", path.size() + "-step" + suffix + " to "
                    + destination.getX() + " " + destination.getY() + " " + destination.getZ());
        }
        return true;
    }

    private static Plan aStar(ClientLevel world, LocalPlayer player, BlockPos start, BlockPos exactGoal, BlockPos requestedGoal) {
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

            for (Edge edge : neighbors(world, player, current)) {
                BlockPos next = edge.pos();
                double tentative = currentG + edge.cost();
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

    private static List<Edge> neighbors(ClientLevel world, LocalPlayer player, BlockPos current) {
        List<Edge> result = new ArrayList<>(14);
        addHorizontalCandidates(world, player, result, current, 1, 0);
        addHorizontalCandidates(world, player, result, current, -1, 0);
        addHorizontalCandidates(world, player, result, current, 0, 1);
        addHorizontalCandidates(world, player, result, current, 0, -1);

        if (isSwimCell(world, current)) {
            addEdge(world, player, result, current, current.above());
            addEdge(world, player, result, current, current.below());
        }
        return result;
    }

    private static void addHorizontalCandidates(
            ClientLevel world,
            LocalPlayer player,
            List<Edge> out,
            BlockPos current,
            int dx,
            int dz
    ) {
        int x = current.getX() + dx;
        int z = current.getZ() + dz;
        addEdge(world, player, out, current, new BlockPos(x, current.getY(), z));
        addEdge(world, player, out, current, new BlockPos(x, current.getY() + 1, z));
        addEdge(world, player, out, current, new BlockPos(x, current.getY() - 1, z));
    }

    private static void addEdge(ClientLevel world, LocalPlayer player, List<Edge> out, BlockPos current, BlockPos target) {
        int dy = target.getY() - current.getY();
        int horizontal = Math.abs(target.getX() - current.getX()) + Math.abs(target.getZ() - current.getZ());

        if (horizontal == 0) {
            if (Math.abs(dy) != 1 || (!isSwimCell(world, current) && !isSwimCell(world, target))) return;
        } else {
            if (horizontal != 1 || Math.abs(dy) > 1) return;
        }

        Edge edge = evaluateEdge(world, player, current, target);
        if (edge != null) out.add(edge);
    }

    private static Edge evaluateEdge(ClientLevel world, LocalPlayer player, BlockPos current, BlockPos target) {
        if (!isLoaded(world, target) || target.getY() < world.getMinY() || target.getY() >= world.getMaxY()) return null;

        double cost = 1.0 + Math.abs(target.getY() - current.getY()) * 0.25;

        for (BlockPos body : List.of(target, target.above())) {
            BlockState state = world.getBlockState(body);
            if (isBodyPassable(world, body, state)) continue;
            if (!isBreakable(world, player, body)) return null;
            cost += 6.0;
        }

        boolean swimming = isSwimCell(world, target);
        if (swimming) {
            cost += 0.45;
        } else if (!hasSafeSupport(world, target.below())) {
            if (!canPlaceSupportAt(world, target.below())) return null;
            if (findPlaceableBlockSlot(player, world, target.below()) < 0) return null;
            cost += 5.0;
        }

        return new Edge(target.immutable(), cost);
    }

    private static BlockPos findCurrentNode(ClientLevel world, BlockPos base, int verticalRadius) {
        if (isCurrentOccupancyValid(world, base)) return base;
        for (int delta = 1; delta <= verticalRadius; delta++) {
            BlockPos up = base.above(delta);
            if (isCurrentOccupancyValid(world, up)) return up;
            BlockPos down = base.below(delta);
            if (isCurrentOccupancyValid(world, down)) return down;
        }
        return null;
    }

    private static boolean isCurrentOccupancyValid(ClientLevel world, BlockPos feet) {
        if (!isLoaded(world, feet)) return false;
        if (!isBodyPassable(world, feet, world.getBlockState(feet))) return false;
        if (!isBodyPassable(world, feet.above(), world.getBlockState(feet.above()))) return false;
        return isSwimCell(world, feet) || hasSafeSupport(world, feet.below());
    }

    private static BlockPos findGoalNear(ClientLevel world, LocalPlayer player, BlockPos base, int verticalRadius) {
        if (canOccupyWithActions(world, player, base)) return base;
        for (int delta = 1; delta <= verticalRadius; delta++) {
            BlockPos up = base.above(delta);
            if (canOccupyWithActions(world, player, up)) return up;
            BlockPos down = base.below(delta);
            if (canOccupyWithActions(world, player, down)) return down;
        }
        return null;
    }

    private static boolean canOccupyWithActions(ClientLevel world, LocalPlayer player, BlockPos feet) {
        if (!isLoaded(world, feet)) return false;
        for (BlockPos body : List.of(feet, feet.above())) {
            BlockState state = world.getBlockState(body);
            if (!isBodyPassable(world, body, state) && !isBreakable(world, player, body)) return false;
        }
        if (isSwimCell(world, feet) || hasSafeSupport(world, feet.below())) return true;
        return canPlaceSupportAt(world, feet.below()) && findPlaceableBlockSlot(player, world, feet.below()) >= 0;
    }

    private static BlockPos firstBlockingBodyBlock(ClientLevel world, LocalPlayer player, BlockPos feet) {
        BlockPos[] positions = {feet, feet.above()};
        for (BlockPos pos : positions) {
            BlockState state = world.getBlockState(pos);
            if (!isBodyPassable(world, pos, state)) return pos;
        }
        return null;
    }

    private static boolean isBodyPassable(ClientLevel world, BlockPos pos, BlockState state) {
        if (isDangerous(state)) return false;
        return state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean isBreakable(ClientLevel world, LocalPlayer player, BlockPos pos) {
        if (!isLoaded(world, pos)) return false;
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || isDangerous(state) || !state.getFluidState().isEmpty()) return false;
        if (state.getCollisionShape(world, pos).isEmpty()) return true;
        return player.hasInfiniteMaterials() || state.getDestroyProgress(player, player.level(), pos) > 0.0F;
    }

    private static boolean isSwimCell(ClientLevel world, BlockPos feet) {
        BlockState feetState = world.getBlockState(feet);
        BlockState headState = world.getBlockState(feet.above());
        return isSafeFluid(feetState) || isSafeFluid(headState);
    }

    private static boolean isSafeFluid(BlockState state) {
        return !state.getFluidState().isEmpty() && !isDangerous(state);
    }

    private static boolean hasSafeSupport(ClientLevel world, BlockPos floorPos) {
        if (!isLoaded(world, floorPos)) return false;
        BlockState floor = world.getBlockState(floorPos);
        if (isDangerous(floor)) return false;
        return !floor.getCollisionShape(world, floorPos).isEmpty();
    }

    private static boolean canPlaceSupportAt(ClientLevel world, BlockPos pos) {
        if (!isLoaded(world, pos)) return false;
        BlockState state = world.getBlockState(pos);
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
                && Math.abs(player.getY() - waypoint.getY()) <= 0.58;
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
                + Math.abs(a.getY() - b.getY()) * 0.45;
    }

    private static void haltMovement() {
        moveForward = false;
        jump = false;
        sneak = false;
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
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

    private record Edge(BlockPos pos, double cost) {
    }

    private record Plan(List<BlockPos> nodes, boolean exact) {
    }

    private record Placement(BlockPos anchor, Direction face) {
    }
}
