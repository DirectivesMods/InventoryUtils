package inventoryutils.proxy;

import net.minecraftforge.common.MinecraftForge;

import inventoryutils.config.Configs;
import inventoryutils.event.InputEventHandler;

public class ClientProxy extends CommonProxy
{
    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new InputEventHandler());
        MinecraftForge.EVENT_BUS.register(new Configs());
    }
}
