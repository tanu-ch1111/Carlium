package carlium.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;

import java.util.List;

public class ParticleOptimizer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
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
                                        if (distance > 10.0) {
                                            shouldBeRemoved = true;
                                        } else {
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
                                if (distance > 10.0) {
                                    shouldBeRemoved = true;
                                } else {
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
}