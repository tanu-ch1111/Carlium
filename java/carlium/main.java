package carlium;

import carlium.optimization.BlockOptimizer;
import carlium.optimization.ParticleOptimizer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = main.MODID,
        name = main.NAME,
        version = main.VERSION,
        acceptedMinecraftVersions = "[1.8.9]"
)
public class main {
    public static final String MODID = "carlium";
    public static final String NAME = "Carlium";
    public static final String VERSION = "Beta";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ParticleOptimizer());
        MinecraftForge.EVENT_BUS.register(new BlockOptimizer());
    }
}