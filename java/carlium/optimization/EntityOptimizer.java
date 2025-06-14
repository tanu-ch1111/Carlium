package carlium.optimization;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import carlium.Config;

public class EntityOptimizer {

    private static double OPTIMIZATION_DISTANCE = 30.0;

    private final Set<UUID> entitiesToHide = new HashSet<>();

    private static boolean enabled = true;

    public static void setEnabled(boolean enabled) {
        EntityOptimizer.enabled = enabled;
    }

    public static void setOptimizationDistance(double distance) {
        OPTIMIZATION_DISTANCE = distance;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) {
            return;
        }

        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        World world = Minecraft.getMinecraft().theWorld;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        ICamera frustum = new Frustum();
        double viewX = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double viewY = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double viewZ = Minecraft.getMinecraft().getRenderManager().viewerPosZ;
        frustum.setPosition(viewX, viewY, viewZ);

        Set<UUID> currentFrameEntitiesToHide = new HashSet<>();

        List<Entity> loadedEntities = world.loadedEntityList;

        for (int i = loadedEntities.size() - 1; i >= 0; i--) {
            Entity entity = loadedEntities.get(i);

            if (entity == player|| entity instanceof EntityPlayer) {
                continue;
            }

            if (entity == null || !entity.isEntityAlive()) {
                continue;
            }

            AxisAlignedBB entityAABB = entity.getEntityBoundingBox();
            if (entityAABB == null) {
                continue;
            }

            boolean shouldBeDisplayed = false;

            double distance = player.getDistanceToEntity(entity);

            boolean isInFrustum = frustum.isBoundingBoxInFrustum(entityAABB);

            boolean isWithinDistance = (distance <= OPTIMIZATION_DISTANCE);

            if (isInFrustum && isWithinDistance) {
                shouldBeDisplayed = true;
            }

            if (!shouldBeDisplayed) {
                currentFrameEntitiesToHide.add(entity.getUniqueID());
            }
        }

        entitiesToHide.clear();
        entitiesToHide.addAll(currentFrameEntitiesToHide);
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        if (!enabled) {
            return;
        }
        if (entitiesToHide.contains(event.entity.getUniqueID())) {
            event.setCanceled(true);
        }
    }
}