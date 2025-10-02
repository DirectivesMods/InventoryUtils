package guiclicker.proxy;

import net.minecraftforge.common.MinecraftForge;

import guiclicker.config.Configs;
import guiclicker.event.InputEventHandler;

public class ClientProxy extends CommonProxy
{
    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new InputEventHandler());
        MinecraftForge.EVENT_BUS.register(new Configs());
    }
}
