package dev.relaylyrics.platform.mc;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 1.8.9 adapter for the world queries the 3D lyrics make through 1.21 APIs
 * ({@code world.raycast}, {@code getLightLevel}, {@code isSkyVisible}, camera yaw/pitch...).
 * The camera is the render-view entity's eye, interpolated with the current frame's partial ticks.
 */
public final class WorldView {
    private static float partialTicks;

    private WorldView() { }

    /** Called at the start of each world-lyrics frame. */
    public static void beginFrame(float ticks) { partialTicks = ticks; }

    private static World world() { return Minecraft.getMinecraft().theWorld; }
    private static Entity viewer() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer;
    }

    /** Interpolated feet position of the view entity (the origin of 1.8.9's world modelview). */
    public static Vec3d renderOrigin() {
        Entity e = viewer();
        return new Vec3d(e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks,
                e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks,
                e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks);
    }

    public static double eyeHeight() { return viewer().getEyeHeight(); }

    /** Eye position; used as 1.21's camera position by the placement logic. */
    public static Vec3d cameraPos() { return renderOrigin().add(0, eyeHeight(), 0); }

    private static boolean frontView() { return Minecraft.getMinecraft().gameSettings.thirdPersonView == 2; }

    public static float cameraYaw() {
        Entity e = viewer();
        float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * partialTicks;
        return frontView() ? yaw + 180 : yaw;
    }

    public static float cameraPitch() {
        Entity e = viewer();
        float pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * partialTicks;
        return frontView() ? -pitch : pitch;
    }

    public static double fov() { return Minecraft.getMinecraft().gameSettings.fovSetting; }
    public static int viewDistance() { return Minecraft.getMinecraft().gameSettings.renderDistanceChunks; }

    public static Vec3d playerPos() {
        EntityPlayer p = Minecraft.getMinecraft().thePlayer;
        return new Vec3d(p.posX, p.posY, p.posZ);
    }

    public static Vec3d playerVelocity() {
        EntityPlayer p = Minecraft.getMinecraft().thePlayer;
        return new Vec3d(p.motionX, p.motionY, p.motionZ);
    }

    public static boolean playerOnGround() { return Minecraft.getMinecraft().thePlayer.onGround; }
    public static boolean playerFlying() { return Minecraft.getMinecraft().thePlayer.capabilities.isFlying; }

    /**
     * 1.21's {@code raycast(COLLIDER, FluidHandling.NONE)}: fluids and blocks without a collision
     * box are passed through. A miss reports the end point, as 1.21 does.
     */
    public static BlockHitResult raycast(Vec3d start, Vec3d end) {
        World world = world();
        MovingObjectPosition hit = world == null ? null : world.rayTraceBlocks(start.handle(), end.handle(), false, true, false);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.getBlockPos() == null) {
            Vec3d direction = end.subtract(start);
            return BlockHitResult.miss(end, facing(-direction.x, -direction.y, -direction.z));
        }
        return BlockHitResult.block(Vec3d.of(hit.hitVec), BlockPos.of(hit.getBlockPos()),
                hit.sideHit == null ? Direction.UP : Direction.of(hit.sideHit));
    }

    private static Direction facing(double x, double y, double z) {
        double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ay >= ax && ay >= az) return y >= 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return x >= 0 ? Direction.EAST : Direction.WEST;
        return z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static boolean isChunkLoaded(net.minecraft.util.BlockPos pos) {
        World world = world();
        return world != null && world.isBlockLoaded(pos);
    }

    /** 1.21 {@code !getFluidState(pos).isEmpty()}. */
    public static boolean hasFluid(net.minecraft.util.BlockPos pos) {
        World world = world();
        return world != null && world.getBlockState(pos).getBlock().getMaterial().isLiquid();
    }

    /** 1.21 {@code getLightLevel(pos)}: combined sky/block light, minus the sky darkening. */
    public static int lightLevel(net.minecraft.util.BlockPos pos) {
        World world = world();
        return world == null ? 15 : world.getLightFromNeighbors(pos);
    }

    public static boolean isSkyVisible(net.minecraft.util.BlockPos pos) {
        World world = world();
        return world != null && world.canSeeSky(pos);
    }

    /** 1.21 {@code isSideSolidFullSquare}: Forge's side-solid check is the 1.8.9 equivalent. */
    public static boolean solidFace(BlockHitResult hit) {
        World world = world();
        if (world == null) return false;
        Material material = world.getBlockState(hit.getBlockPos()).getBlock().getMaterial();
        return material.isSolid() && world.isSideSolid(hit.getBlockPos(), hit.getSide().handle(), false);
    }

    public static Object worldIdentity() { return world(); }
}
