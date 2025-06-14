package carlium.optimization;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;

import java.util.List;

import carlium.Config;

public class ParticleOptimizer {

    private static boolean enabled = true;

    public static void setEnabled(boolean enabled) {
        ParticleOptimizer.enabled = enabled;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) {
            return;
        }

        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        EffectRenderer effectRenderer = Minecraft.getMinecraft().effectRenderer;

        Object rawFxLayers;
        try {
            rawFxLayers = ReflectionHelper.getPrivateValue(EffectRenderer.class, effectRenderer, "fxLayers", "field_78876_b");
        } catch (Exception e) {
            System.err.println("[" + this.getClass().getName() + ":" + Thread.currentThread().getStackTrace()[1].getMethodName() + ":" + Thread.currentThread().getStackTrace()[1].getLineNumber() + "]: Failed to access fxLayers: " + e.getMessage());
            return;
        }

        if (rawFxLayers == null) {
            return;
        }

        ICamera frustum = new Frustum();
        double viewX = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double viewY = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double viewZ = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
        frustum.setPosition(viewX, viewY, viewZ);

        World world = Minecraft.getMinecraft().theWorld;
        Vec3 playerEyePos = new Vec3(
                Minecraft.getMinecraft().thePlayer.posX,
                Minecraft.getMinecraft().thePlayer.posY + Minecraft.getMinecraft().thePlayer.getEyeHeight(),
                Minecraft.getMinecraft().thePlayer.posZ
        );


        if (rawFxLayers instanceof Object[]) {
            Object[] fxLayersOuterArray = (Object[]) rawFxLayers;

            for (int i = 0; i < fxLayersOuterArray.length; i++) {
                Object innerLayerObject = fxLayersOuterArray[i];
                if (innerLayerObject == null) continue;

                if (innerLayerObject instanceof Object[]) {
                    Object[] particlesLayerArray = (Object[]) innerLayerObject;

                    for (Object obj : particlesLayerArray) {
                        if (obj == null) continue;

                        if (obj instanceof List) {
                            List<EntityFX> particlesInLayer = (List<EntityFX>) obj;

                            for (int k = particlesInLayer.size() - 1; k >= 0; k--) {
                                Object particleObj = particlesInLayer.get(k);

                                if (particleObj instanceof EntityFX) {
                                    EntityFX particle = (EntityFX) particleObj;

                                    boolean isInFrustum = false;

                                    AxisAlignedBB particleAABB = particle.getEntityBoundingBox();

                                    if (frustum.isBoundingBoxInFrustum(particleAABB)) {
                                        isInFrustum = true;
                                    }

                                    double distance = Minecraft.getMinecraft().thePlayer.getDistanceToEntity(particle);

                                    boolean shouldBeRemoved = false;

                                    if (isInFrustum) {
                                        if (distance > Config.entityOptimizerDistance) {
                                            shouldBeRemoved = true;
                                        } else {
                                            if (isBlockBetween(world, playerEyePos, particle.getPositionVector())) {
                                                shouldBeRemoved = true;
                                            }
                                        }
                                    } else {
                                        shouldBeRemoved = true;
                                    }

                                    if (shouldBeRemoved) {
                                        particle.setDead();
                                    }
                                }
                            }
                        } else {
                            System.err.println("[" + this.getClass().getName() + ":" + Thread.currentThread().getStackTrace()[1].getMethodName() + ":" + Thread.currentThread().getStackTrace()[1].getLineNumber() + "]: Element within inner array is not a List: " + obj.getClass().getName());
                        }
                    }
                } else if (innerLayerObject instanceof List) {
                    List<EntityFX> particlesInLayer = (List<EntityFX>) innerLayerObject;

                    for (int k = particlesInLayer.size() - 1; k >= 0; k--) {
                        Object obj = particlesInLayer.get(k);

                        if (obj instanceof EntityFX) {
                            EntityFX particle = (EntityFX) obj;

                            boolean isInFrustum = false;

                            AxisAlignedBB particleAABB = particle.getEntityBoundingBox();
                            if (frustum.isBoundingBoxInFrustum(particleAABB)) {
                                isInFrustum = true;
                            }

                            double distance = Minecraft.getMinecraft().thePlayer.getDistanceToEntity(particle);

                            boolean shouldBeRemoved = false;

                            if (isInFrustum) {
                                if (distance > Config.entityOptimizerDistance) {
                                    shouldBeRemoved = true;
                                } else {
                                    if (isBlockBetween(world, playerEyePos, particle.getPositionVector())) {
                                        shouldBeRemoved = true;
                                    }
                                }
                            } else {
                                shouldBeRemoved = true;
                            }

                            if (shouldBeRemoved) {
                                particle.setDead();
                            }
                        }
                    }
                } else {
                    System.err.println("[" + this.getClass().getName() + ":" + Thread.currentThread().getStackTrace()[1].getMethodName() + ":" + Thread.currentThread().getStackTrace()[1].getLineNumber() + "]: Element in fxLayersOuterArray is neither List[] nor List: " + innerLayerObject.getClass().getName());
                }
            }
        } else {
            System.err.println("[" + this.getClass().getName() + ":" + Thread.currentThread().getStackTrace()[1].getMethodName() + ":" + Thread.currentThread().getStackTrace()[1].getLineNumber() + "]: fxLayers is not an Object[] type: " + rawFxLayers.getClass().getName());
        }
    }

    private boolean isBlockBetween(World world, Vec3 start, Vec3 end) {
        MovingObjectPosition mop = world.rayTraceBlocks(start, end, false, true, false);

        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block = world.getBlockState(mop.getBlockPos()).getBlock();

            if (block.getMaterial() == Material.air ||
                    block.getMaterial() == Material.water ||
                    block.getMaterial() == Material.lava ||
                    block instanceof BlockGlass ||
                    block instanceof BlockStainedGlass ||
                    block instanceof BlockPane ||
                    block instanceof BlockStainedGlassPane ||
                    block instanceof BlockSlab ||
                    block instanceof BlockTrapDoor ||
                    !block.isFullCube() ||
                    !block.isOpaqueCube()
            ) {
                return false;
            }
            return true;
        }
        return false;
    }
}