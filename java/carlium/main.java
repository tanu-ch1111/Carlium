package carlium;

import carlium.optimization.BlockOptimizer;
import carlium.optimization.EntityOptimizer;
import carlium.optimization.ParticleOptimizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Mod(
        modid = main.MODID,
        name = main.NAME,
        version = main.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        guiFactory = "carlium.ConfigGuiFactory"
)
public class main {
    public static final String MODID = "carlium";
    public static final String NAME = "Carlium";
    public static final String VERSION = "Beta1.0.2";
    private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/tanu-ch1111/Carlium/refs/heads/main/version.json";

    private String lastCheckedServerHost = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ParticleOptimizer particleOptimizer;
    private BlockOptimizer blockOptimizer;
    private EntityOptimizer entityOptimizer;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.init(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        particleOptimizer = new ParticleOptimizer();
        blockOptimizer = new BlockOptimizer();
        entityOptimizer = new EntityOptimizer();

        MinecraftForge.EVENT_BUS.register(particleOptimizer);
        MinecraftForge.EVENT_BUS.register(blockOptimizer);
        MinecraftForge.EVENT_BUS.register(entityOptimizer);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Config());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        applyConfigSettings();
    }

    private void applyConfigSettings() {
        ParticleOptimizer.setEnabled(Config.particleOptimizerEnabled);
        EntityOptimizer.setEnabled(Config.entityOptimizerEnabled);
        EntityOptimizer.setOptimizationDistance(Config.entityOptimizerDistance);
        BlockOptimizer.setEnabled(Config.blockOptimizerEnabled);
        BlockOptimizer.setScanRangeChunk(Config.blockOptimizerScanRangeChunk);
        BlockOptimizer.setOptimizationChunkUpdateIntervalTicks(Config.blockOptimizerUpdateIntervalTicks);
        BlockOptimizer.setMaxOptimizedBlocksPerChunk(Config.blockOptimizerMaxOptimizedBlocksPerChunk);
    }


    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerData currentServerData = Minecraft.getMinecraft().getCurrentServerData();
        String currentServerHost = (currentServerData != null) ? currentServerData.serverIP : "singleplayer";

        if (!currentServerHost.equals(lastCheckedServerHost)) {
            new Thread(this::checkVersionAndNotify).start();
            lastCheckedServerHost = currentServerHost;
        }
    }

    private void checkVersionAndNotify() {
        try {
            URL url = new URL(VERSION_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                connection.disconnect();

                JsonElement jsonElement = new JsonParser().parse(content.toString());
                String remoteVersion = jsonElement.getAsJsonObject().get("version").getAsString();

                if (!VERSION.equals(remoteVersion)) {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§6[Carlium] Your client version (§f" + VERSION + "§6) is outdated! Please update to §f" + remoteVersion + "§6."));
                        }
                    });
                } else {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[Carlium] Your client is up to date! (§f" + VERSION + "§a)"));
                        }
                    });
                }
            } else {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[Carlium] Could not check for updates. HTTP error code: " + responseCode));
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[Carlium] Failed to check for updates: " + e.getMessage());
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§e[Carlium] Failed to check for updates. Please check your internet connection."));
                }
            });
        }
    }
}
