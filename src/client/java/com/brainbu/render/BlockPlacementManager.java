package com.brainbu.render;

import com.brainbu.config.CpvpCompanionConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.brainbu.CpvpCompanionMod.LOGGER;
import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.ceil;
import static net.minecraft.util.math.MathHelper.floor;

public class BlockPlacementManager {
    public record Face(BlockPos blockPos, Direction direction) {
    }

    private static final List<BlockPos> reachableBlocks = new ArrayList<>();
    private static final List<Face> placeableFaces = new ArrayList<>();
    private static long lastUpdateTick = 0;
    private static final int UPDATE_INTERVAL = 5; // tick

    public static void endClientTick(MinecraftClient client) {
    }

    private static void computeFaces(Frustum frustum) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (player == null || world == null) return;
        boolean playerIdle = player.getMovementSpeed() < 0.01 &&
                fuzzyEqual(player.getYaw(), player.prevYaw, 1e-7f) &&
                fuzzyEqual(player.getPitch(), player.prevPitch, 1e-7f);

        if (world.getTime() - lastUpdateTick < UPDATE_INTERVAL || playerIdle) return;
        lastUpdateTick = world.getTime();
        reachableBlocks.clear();
        placeableFaces.clear();

        double reach = player.getBlockInteractionRange();
        Vector3f eyePos = player.getEyePos().toVector3f();

        for (int x = floor(eyePos.x-reach); x <= ceil(eyePos.x+reach); x++) {
            for (int y = floor(eyePos.y-reach); y <= ceil(eyePos.y+reach); y++) {
                for (int z = floor(eyePos.z-reach); z <= ceil(eyePos.z+reach); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).getBlock() != Blocks.AIR &&
                            frustum.isVisible(new Box(pos))
                    ) {
                        reachableBlocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

//        HitResult hit = player.raycast(5.0, 0.0f, false);
//        if (hit.getType() != HitResult.Type.BLOCK) return;
//
//        BlockHitResult blockHit = (BlockHitResult) hit;
//        BlockPos face = blockHit.getBlockPos();
//        Direction side = blockHit.getSide();



        // Check all adjacent faces of the clicked block
        for (BlockPos blockPos : reachableBlocks) {
            for (Direction direction : Direction.values()) {
                if (canPlaceBlockAt(world, blockPos, direction, player, reach)) {
                    placeableFaces.add(new Face(blockPos, direction));
                }
            }
        }

        LOGGER.info("Got " + placeableFaces.size() + " faces for " + reachableBlocks.size() + " blocks");
    }

    private static boolean fuzzyEqual(float a, float b, float tol) {
        return abs(a - b) < tol;
    }

    // this helper function assumes the block to be placed is Full Cube
    // entity collision and reach
    private static boolean canPlaceBlockAt(ClientWorld world, BlockPos pos, Direction direction, PlayerEntity player, double reach) {
        // an imprecise check of reach
        Vec3d negative = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        boolean inReach = false;
        for (int i = 0; i < 8; ++i) {
            if (negative.add(i & 1, i & 2, i & 4).isInRange(player.getEyePos(), reach)) {
                inReach = true;
                break;
            }
        }
        if (!inReach) return false;

        // placeable
        BlockState self = world.getBlockState(pos);
        BlockState next = world.getBlockState(pos.offset(direction));

        // Outline shape is also use for placing blocks in vanilla
        VoxelShape selfShape = self.getOutlineShape(world, pos);
        VoxelShape nextShape = next.getOutlineShape(world, pos);

        boolean covered = VoxelShapes.isSideCovered(selfShape, nextShape, direction);

        Box boxToPlace = null;

        // TODO render actual shapes of blocks
        if (!self.isFullCube(world, pos)) {
            return false;
        }

        if (self.isReplaceable()) {
//            boxToPlace = new Box(pos);
            return false;
        } else if (next.isReplaceable()) {
            boxToPlace = new Box(pos.offset(direction));
        } else if (covered) {
            boxToPlace = new Box(pos.offset(direction));
            return false;
        } else {
            return false;
        }

        // placeable - entity collision - as if the placed block is full cube
        if (!world.doesNotIntersectEntities(null, VoxelShapes.cuboid(boxToPlace))) {
            return false;
        }

        return true;
    }

    // TODO Cache or combine the faces
    public static void renderWorld(WorldRenderContext worldContext) {
        if (!CpvpCompanionConfig.INSTANCE.blockPlacement.enabled) return;
        computeFaces(worldContext.frustum());

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
//        if (MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(player.getUuid()).getGameMode() != GameMode.SURVIVAL) return;

        Vec3d cameraPos = worldContext.camera().getPos();

        // rendering states
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        MatrixStack matrices = worldContext.matrixStack();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (Face face : placeableFaces) {
            renderFace(worldContext, matrices, face);
        }

        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

    }

    private static void renderFace(WorldRenderContext context, MatrixStack matrices, Face face) {
//        ClientWorld world = worldContext.world();
        Vector3f blockPos = face.blockPos().toCenterPos().toVector3f().sub(0.5f, 0.5f, 0.5f);
        Direction direction = face.direction();

        MatrixStack.Entry matrix = matrices.peek();

        int fillColor = CpvpCompanionConfig.INSTANCE.blockPlacement.fillColor;
        int outlineColor = CpvpCompanionConfig.INSTANCE.blockPlacement.outlineColor;

//        VertexConsumer lineConsumer = context.consumers().getBuffer(RenderLayer.getLines());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer;

        Vector3f[] vertFace = CG.add(faceVertices(direction), blockPos);
        Vector3f[] vertOutline = CG.add(outlineVertices(direction), blockPos);
//        Matrix4f position = context.positionMatrix();
//        Matrix4f projection = context.projectionMatrix().invertPerspective();
//        Matrix4f projection = context.gameRenderer().getBasicProjectionMatrix(MinecraftClient.getInstance().options.getFov().getValue());

//        Vector3f[] vertFaceCamera = CG.cameraFromWorld(context.camera(), vertFace);
//        Vector3f[] vertOutlineCamera = CG.cameraFromWorld(context.camera(), vertOutline);

//        Vector3f[] faceProjected = CG.screenFromCameraCoords(projection, vertFaceCamera);
//        Vector3f[] outlineProjected = CG.screenFromCameraCoords(projection, vertOutlineCamera);
//
//        Vector3f[] faceProjected = CG.screenFromWorld(position, projection, vertFace);
//        Vector3f[] outlineProjected = CG.screenFromWorld(position, projection, vertOutline);

        // Render face
        buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        addVerticesPC(buffer, matrix, vertFace, fillColor);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Render outline
//        RenderSystem.lineWidth(2.0f);
//        if (((outlineColor >> 24) & 0xFF) / 255 > 0) { // alpha non-zero
//            buffer = tessellator.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
//            addVerticesPTCN(buffer, matrix, vertOutline, outlineColor);
//            BufferRenderer.drawWithGlobalProgram(buffer.end());
//        }

    }

    private static Vector3f[] faceVertices(Direction direction) {
        Vector3f[] res = new Vector3f[4];
        switch (direction) {
            case UP:
                res[0] = new Vector3f(0, 1, 1);
                res[3] = new Vector3f(0, 1, 0);
                res[2] = new Vector3f(1, 1, 0);
                res[1] = new Vector3f(1, 1, 1);
                break;
            case DOWN:
                res[0] = new Vector3f(0, 0, 0);
                res[1] = new Vector3f(1, 0, 0);
                res[2] = new Vector3f(1, 0, 1);
                res[3] = new Vector3f(0, 0, 1);
                break;
            case NORTH:
                res[0] = new Vector3f(0, 0, 0);
                res[1] = new Vector3f(1, 0, 0);
                res[2] = new Vector3f(1, 1, 0);
                res[3] = new Vector3f(0, 1, 0);
                break;
            case SOUTH:
                res[0] = new Vector3f(1, 0, 1);
                res[1] = new Vector3f(0, 0, 1);
                res[2] = new Vector3f(0, 1, 1);
                res[3] = new Vector3f(1, 1, 1);
                break;
            case WEST:
                res[0] = new Vector3f(0, 0, 0);
                res[1] = new Vector3f(0, 0, 1);
                res[2] = new Vector3f(0, 1, 1);
                res[3] = new Vector3f(0, 1, 0);
                break;
            case EAST:
                res[0] = new Vector3f(1, 0, 1);
                res[1] = new Vector3f(1, 0, 0);
                res[2] = new Vector3f(1, 1, 0);
                res[3] = new Vector3f(1, 1, 1);
                break;
        }
        return res;
    }

    private static Vector3f[] outlineVertices(Direction direction) {
        Vector3f[] res = new Vector3f[5];
        switch (direction) {
            case UP:
                res[0] = new Vector3f(0, 1, 1);
                res[1] = new Vector3f(1, 1, 1);
                res[2] = new Vector3f(1, 1, 0);
                res[3] = new Vector3f(0, 1, 0);
                res[4] = new Vector3f(0, 1, 1);
                break;
            case DOWN:
                res[0] = new Vector3f(0, 0, 0);
                res[1] = new Vector3f(1, 0, 0);
                res[2] = new Vector3f(1, 0, 1);
                res[3] = new Vector3f(0, 0, 1);
                res[4] = new Vector3f(0, 0, 0);
                break;
            case NORTH:
                res[0] = new Vector3f(0, 0, 0);
                res[1] = new Vector3f(1, 0, 0);
                res[2] = new Vector3f(1, 1, 0);
                res[3] = new Vector3f(0, 1, 0);
                res[4] = new Vector3f(0, 0, 0);
                break;
            case SOUTH:
                res[0] = new Vector3f(1, 0, 1);
                res[1] = new Vector3f(0, 0, 1);
                res[2] = new Vector3f(0, 1, 1);
                res[3] = new Vector3f(1, 1, 1);
                res[4] = new Vector3f(1, 0, 1);
                break;
            case WEST:
                res[0] = new Vector3f(0, 0, 0);
                res[1] = new Vector3f(0, 0, 1);
                res[2] = new Vector3f(0, 1, 1);
                res[3] = new Vector3f(0, 1, 0);
                res[4] = new Vector3f(0, 0, 0);
                break;
            case EAST:
                res[0] = new Vector3f(1, 0, 1);
                res[1] = new Vector3f(1, 0, 0);
                res[2] = new Vector3f(1, 1, 0);
                res[3] = new Vector3f(1, 1, 1);
                res[4] = new Vector3f(1, 0, 1);
                break;
        }
        return res;
    }

    private static void addVerticesPC(BufferBuilder bufferBuilder, MatrixStack.Entry matrix, Vector3f[] xs, int argb) {
        for (Vector3f x : xs) {
            bufferBuilder.vertex(matrix, x).color(argb);
        }
    }
    private static void addVerticesPTCN(BufferBuilder bufferBuilder, MatrixStack.Entry matrix, Vector3f[] xs, int color) {
        for (Vector3f x : xs) {
            Vector3f normal = new Vector3f(x).normalize();
            bufferBuilder.vertex(matrix, x).texture(0, 0).color(color).normal(normal.x, normal.y, normal.z);
        }
    }

    private static void addFaceVertices(BufferBuilder buffer, Direction direction, int c) {
        switch (direction) {
            case UP:
                buffer.vertex(0, 1, 1).color(c);
                buffer.vertex(1, 1, 1).color(c);
                buffer.vertex(1, 1, 0).color(c);
                buffer.vertex(0, 1, 0).color(c);
                break;
            case DOWN:
                buffer.vertex(0, 0, 0).color(c);
                buffer.vertex(1, 0, 0).color(c);
                buffer.vertex(1, 0, 1).color(c);
                buffer.vertex(0, 0, 1).color(c);
                break;
            case NORTH:
                buffer.vertex(0, 0, 0).color(c);
                buffer.vertex(1, 0, 0).color(c);
                buffer.vertex(1, 1, 0).color(c);
                buffer.vertex(0, 1, 0).color(c);
                break;
            case SOUTH:
                buffer.vertex(1, 0, 1).color(c);
                buffer.vertex(0, 0, 1).color(c);
                buffer.vertex(0, 1, 1).color(c);
                buffer.vertex(1, 1, 1).color(c);
                break;
            case WEST:
                buffer.vertex(0, 0, 0).color(c);
                buffer.vertex(0, 0, 1).color(c);
                buffer.vertex(0, 1, 1).color(c);
                buffer.vertex(0, 1, 0).color(c);
                break;
            case EAST:
                buffer.vertex(1, 0, 1).color(c);
                buffer.vertex(1, 0, 0).color(c);
                buffer.vertex(1, 1, 0).color(c);
                buffer.vertex(1, 1, 1).color(c);
                break;
        }
    }

    private static void addFaceOutlineVertices(BufferBuilder buffer, Direction direction, int c) {
        switch (direction) {
            case UP:
                buffer.vertex(0, 1, 1).color(c);
                buffer.vertex(1, 1, 1).color(c);
                buffer.vertex(1, 1, 0).color(c);
                buffer.vertex(0, 1, 0).color(c);
                buffer.vertex(0, 1, 1).color(c);
                break;
            case DOWN:
                buffer.vertex(0, 0, 0).color(c);
                buffer.vertex(1, 0, 0).color(c);
                buffer.vertex(1, 0, 1).color(c);
                buffer.vertex(0, 0, 1).color(c);
                buffer.vertex(0, 0, 0).color(c);
                break;
            case NORTH:
                buffer.vertex(0, 0, 0).color(c);
                buffer.vertex(1, 0, 0).color(c);
                buffer.vertex(1, 1, 0).color(c);
                buffer.vertex(0, 1, 0).color(c);
                buffer.vertex(0, 0, 0).color(c);
                break;
            case SOUTH:
                buffer.vertex(1, 0, 1).color(c);
                buffer.vertex(0, 0, 1).color(c);
                buffer.vertex(0, 1, 1).color(c);
                buffer.vertex(1, 1, 1).color(c);
                buffer.vertex(1, 0, 1).color(c);
                break;
            case WEST:
                buffer.vertex(0, 0, 0).color(c);
                buffer.vertex(0, 0, 1).color(c);
                buffer.vertex(0, 1, 1).color(c);
                buffer.vertex(0, 1, 0).color(c);
                buffer.vertex(0, 0, 0).color(c);
                break;
            case EAST:
                buffer.vertex(1, 0, 1).color(c);
                buffer.vertex(1, 0, 0).color(c);
                buffer.vertex(1, 1, 0).color(c);
                buffer.vertex(1, 1, 1).color(c);
                buffer.vertex(1, 0, 1).color(c);
                break;
        }
    }

    // math helper
    static class CG {

        static Vector3f[] screenFromWorld(Matrix4f position, Matrix4f projection, Vector3f[] xs) {
            return CG.normalize(CG.mapMul(projection, CG.mapMul(position, CG.lift(xs))));
        }

        static Vector3f[] cameraFromWorld(Camera cam, Vector3f[] xs) {
            Vector3f[] diff = add(xs, cam.getPos().toVector3f().mul(-1));
            Matrix3f rot = new Matrix3f();
            cam.getRotation().get(rot);
            return mapMul(rot, diff);
        }

        static Vector3f[] screenFromCameraCoords(Matrix4f projection, Vector3f[] xs) {
            return CG.normalize(CG.mapMul(projection, CG.lift(xs)));
        }

        static Vector4f[] lift(Vector3f[] xs) {
            return Arrays.stream(xs)
                    .map(x -> new Vector4f(x, 1))
                    .toArray(Vector4f[]::new);
        }

        static Vector3f[] normalize(Vector4f[] xs) {
            return Arrays.stream(xs)
                    .map(x -> new Vector3f(x.x / x.w, x.y / x.w, x.z / x.w))
                    .toArray(Vector3f[]::new);
        }

        static Vector3f[] add(Vector3f[] xs, Vector3f[] ys) {
            assert xs.length == ys.length;
            Vector3f[] res = new Vector3f[max(xs.length, ys.length)];
            for (int i = 0; i < min(xs.length, ys.length); i++) {
                res[i] = xs[i].add(ys[i]);
            }
//            if (xs.length < ys.length) {
//                System.arraycopy(ys, xs.length, res, xs.length, ys.length - xs.length);
//            }
//            if (xs.length > ys.length) {
//                System.arraycopy(xs, ys.length, res, ys.length, xs.length - ys.length);
//            }
            return res;
        }
        static Vector3f[] sub(Vector3f[] xs, Vector3f[] ys) {
            Vector3f[] res = new Vector3f[max(xs.length, ys.length)];
            assert xs.length == ys.length;
            for (int i = 0; i < min(xs.length, ys.length); i++) {
                res[i] = xs[i].sub(ys[i]);
            }
//            if (xs.length < ys.length) {
//                System.arraycopy(ys, xs.length, res, xs.length, ys.length - xs.length);
//            }
//            if (xs.length > ys.length) {
//                System.arraycopy(xs, ys.length, res, ys.length, xs.length - ys.length);
//            }
            return res;
        }

        static Vector3f[] add(Vector3f[] xs, Vector3f y) {
            return Arrays.stream(xs)
                    .map(x -> x.add(y))
                    .toArray(Vector3f[]::new);
        }

        static Vector4f[] mapMul(Matrix4f mat, Vector4f[] xs) {
            return Arrays.stream(xs)
                    .map(mat::transform)
                    .toArray(Vector4f[]::new);
        }

        static Vector3f[] mapMul(Matrix3f mat, Vector3f[] xs) {
            return Arrays.stream(xs)
                    .map(mat::transform)
                    .toArray(Vector3f[]::new);
        }

//        static Matrix4f translationMat(Vector3f x) {
//            Matrix4f.
//        }
    }
}