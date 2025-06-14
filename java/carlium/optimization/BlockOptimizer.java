package carlium.optimization;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.ChunkEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import carlium.Config;

public class BlockOptimizer {

    private static int SCAN_RANGE_CHUNK = 2;
    private static int OPTIMIZATION_CHUNK_UPDATE_INTERVAL_TICKS = 60;
    private static int MAX_OPTIMIZED_BLOCKS_PER_CHUNk = 200;

    private static boolean enabled = true;

    private final ConcurrentHashMap<ChunkCoordIntPair, Set<BlockPos>> optimizedBlocksInChunks = new ConcurrentHashMap<>();

    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor();

    private int tickCounter = 0;

    public static void setEnabled(boolean enabled) {
        BlockOptimizer.enabled = enabled;
    }

    public static void setScanRangeChunk(int scanRangeChunk) {
        SCAN_RANGE_CHUNK = scanRangeChunk;
    }

    public static void setOptimizationChunkUpdateIntervalTicks(int intervalTicks) {
        OPTIMIZATION_CHUNK_UPDATE_INTERVAL_TICKS = intervalTicks;
    }

    public static void setMaxOptimizedBlocksPerChunk(int maxBlocksPerChunk) {
        MAX_OPTIMIZED_BLOCKS_PER_CHUNk = maxBlocksPerChunk;
    }


    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!enabled) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getMinecraft();
            World world = mc.theWorld;
            if (world == null || mc.thePlayer == null) return;

            tickCounter++;

            if (tickCounter % OPTIMIZATION_CHUNK_UPDATE_INTERVAL_TICKS == 0) {
                scanNearbyChunksForOptimization(world, mc.thePlayer.getPosition());
                tickCounter = 0;
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!enabled) {
            return;
        }
        if (event.world.isRemote) {
            scanChunkForOptimization(event.world, event.getChunk().xPosition, event.getChunk().zPosition);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!enabled) {
            return;
        }
        if (event.world.isRemote) {
            optimizedBlocksInChunks.remove(new ChunkCoordIntPair(event.getChunk().xPosition, event.getChunk().zPosition));
        }
    }

    private void scanNearbyChunksForOptimization(World world, BlockPos playerPos) {
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        for (int x = -SCAN_RANGE_CHUNK; x <= SCAN_RANGE_CHUNK; x++) {
            for (int z = -SCAN_RANGE_CHUNK; z <= SCAN_RANGE_CHUNK; z++) {
                int chunkX = playerChunkX + x;
                int chunkZ = playerChunkZ + z;
                if (world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
                    scanChunkForOptimization(world, chunkX, chunkZ);
                }
            }
        }
    }

    private void scanChunkForOptimization(World world, int chunkX, int chunkZ) {
        scanExecutor.submit(() -> {
            Set<BlockPos> currentChunkOptimizedBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());
            int count = 0;

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < world.getHeight(); y++) {
                        BlockPos pos = new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
                        Block block = world.getBlockState(pos).getBlock();

                        if (block instanceof BlockBed) continue;

                        if (!block.isFullCube() || block.getTickRandomly()) {
                            continue;
                        }

                        if (isFullySurrounded(world, pos)) {
                            currentChunkOptimizedBlocks.add(new BlockPos(x, y, z));
                            count++;
                            if (count >= MAX_OPTIMIZED_BLOCKS_PER_CHUNk) {
                                break;
                            }
                        }
                    }
                    if (count >= MAX_OPTIMIZED_BLOCKS_PER_CHUNk) break;
                }
                if (count >= MAX_OPTIMIZED_BLOCKS_PER_CHUNk) break;
            }

            ChunkCoordIntPair chunkCoord = new ChunkCoordIntPair(chunkX, chunkZ);
            optimizedBlocksInChunks.put(chunkCoord, currentChunkOptimizedBlocks);
        });
    }

    public boolean shouldSkipRendering(World world, BlockPos globalPos) {
        if (!enabled) {
            return false;
        }

        ChunkCoordIntPair chunkCoord = new ChunkCoordIntPair(globalPos.getX() >> 4, globalPos.getZ() >> 4);
        Set<BlockPos> optimizedBlocks = optimizedBlocksInChunks.get(chunkCoord);
        if (optimizedBlocks != null) {
            BlockPos relativePos = new BlockPos(globalPos.getX() & 15, globalPos.getY(), globalPos.getZ() & 15);
            return optimizedBlocks.contains(relativePos);
        }
        return false;
    }


    private boolean isFullBlock(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        Block block = world.getBlockState(pos).getBlock();
        return block != null && block.isFullCube();
    }

    private boolean isFullySurrounded(World world, BlockPos pos) {
        return isFullBlock(world, pos.north()) &&
                isFullBlock(world, pos.south()) &&
                isFullBlock(world, pos.east()) &&
                isFullBlock(world, pos.west()) &&
                isFullBlock(world, pos.up()) &&
                isFullBlock(world, pos.down());
    }

    public void shutdown() {
        scanExecutor.shutdown();
        try {
            if (!scanExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scanExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scanExecutor.shutdownNow();
        }
    }
}