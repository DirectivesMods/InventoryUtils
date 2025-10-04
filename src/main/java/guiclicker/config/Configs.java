package guiclicker.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import guiclicker.Reference;

public class Configs
{
    public static boolean enableDragMovingShiftLeft;
    public static boolean enableDragMovingShiftRight;
    public static boolean enableDragMovingControlLeft;
    public static boolean enableDropStack;
    public static boolean enableDropAllOfType;

    public static File configurationFile;
    public static Configuration config;
    
    public static final String CATEGORY_GENERIC = "Generic";

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.modID) == true)
        {
            loadConfigs(config);
        }
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        config = new Configuration(configFile, null, true);
        config.load();

        loadConfigs(config);
    }

    public static void loadConfigs(Configuration conf)
    {
        Property prop;

        prop = conf.get(CATEGORY_GENERIC, "enableDragMovingShiftLeft", true).setRequiresMcRestart(false);
        prop.comment = "Enable moving full stacks of items by holding down Shift and dragging over slots with the left mouse button held down.";
        enableDragMovingShiftLeft = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDragMovingShiftRight", true).setRequiresMcRestart(false);
        prop.comment = "Enable moving everything but the last item from all stacks by holding down Shift and dragging over slots with the right mouse button held down.";
        enableDragMovingShiftRight = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDragMovingControlLeft", true).setRequiresMcRestart(false);
        prop.comment = "Enable moving one item from all stacks by holding down Control and dragging over slots with the left mouse button held down.";
        enableDragMovingControlLeft = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDropStack", true).setRequiresMcRestart(false);
        prop.comment = "Enable dropping entire stack with Ctrl+Q (Cmd+Q on Mac).";
        enableDropStack = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableDropAllOfType", true).setRequiresMcRestart(false);
        prop.comment = "Enable dropping all items of same type with Ctrl+Shift+Q (Cmd+Shift+Q on Mac).";
        enableDropAllOfType = prop.getBoolean();

        if (conf.hasChanged() == true)
        {
            conf.save();
        }
    }
}
