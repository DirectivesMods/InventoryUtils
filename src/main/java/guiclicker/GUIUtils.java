package guiclicker;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import guiclicker.config.Configs;
import guiclicker.proxy.CommonProxy;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "guiclicker.config.GUIUtilsGuiFactory",
    dependencies = "required-after:Forge@[11.15.0.1716,);",
    clientSideOnly=true, acceptedMinecraftVersions = "1.8.9")
public class GUIUtils
{
    @Instance(Reference.MOD_ID)
    public static GUIUtils instance;

    @SidedProxy(clientSide = "guiclicker.proxy.ClientProxy", serverSide = "guiclicker.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        instance = this;
        logger = event.getModLog();
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        proxy.registerEventHandlers();
    }
}
