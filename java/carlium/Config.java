package carlium;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public class Config {

    public static boolean particleOptimizerEnabled = true;
    public static boolean entityOptimizerEnabled = true;
    public static double entityOptimizerDistance = 30.0;
    public static boolean blockOptimizerEnabled = true;
    public static int blockOptimizerScanRangeChunk = 2;
    public static int blockOptimizerUpdateIntervalTicks = 60;
    public static int blockOptimizerMaxOptimizedBlocksPerChunk = 200;

    private static Configuration config;

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    private static void loadConfig() {
        particleOptimizerEnabled = config.getBoolean("ParticleOptimizer", "Optimizers", particleOptimizerEnabled, "Enable particle optimization. (true/false)");

        entityOptimizerEnabled = config.getBoolean("EntityOptimizer", "Optimizers", entityOptimizerEnabled, "Enable entity optimization. (true/false)");
        entityOptimizerDistance = config.getFloat("EntityOptimizer_distance", "Optimizers", (float) entityOptimizerDistance, 1.0F, 128.0F, "Maximum distance for entity optimization (blocks).");

        blockOptimizerEnabled = config.getBoolean("BlockOptimizer", "Optimizers", blockOptimizerEnabled, "Enable block optimization. (true/false)");
        blockOptimizerScanRangeChunk = config.getInt("BlockOptimizer_scan_range_chunk", "Optimizers", blockOptimizerScanRangeChunk, 0, 10, "Radius of chunk scan for block optimization (chunks).");
        blockOptimizerUpdateIntervalTicks = config.getInt("BlockOptimizer_optimization_chunk_update_interval_ticks", "Optimizers", blockOptimizerUpdateIntervalTicks, 20, 600, "Interval for updating optimized chunks (ticks). 20 ticks = 1 second.");
        blockOptimizerMaxOptimizedBlocksPerChunk = config.getInt("BlockOptimizer_max_optimized_blocks_per_chunk", "Optimizers", blockOptimizerMaxOptimizedBlocksPerChunk, 1, 1000, "Maximum number of optimized blocks per chunk.");

        if (config.hasChanged()) {
            config.save();
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(main.MODID)) {
            loadConfig();
        }
    }
}