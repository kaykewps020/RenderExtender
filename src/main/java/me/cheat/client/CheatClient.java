package me.cheat.client;

import me.cheat.client.events.Event;
import me.cheat.client.modules.ModuleManager;
import me.cheat.client.ui.ClickGUI;
import me.cheat.client.utils.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

@Mod(modid = CheatClient.MODID, name = CheatClient.NAME, version = CheatClient.VERSION)
public class CheatClient {
    public static final String MODID = "renderextender";
    public static final String NAME = "RenderExtender";
    public static final String VERSION = "2.0";

    public static final Minecraft mc = Minecraft.getMinecraft();
    public static CheatClient instance;
    public static final Event.EventBus EVENT_BUS = new Event.EventBus();

    private static KeyBinding guiKey;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        System.out.println("[RenderExtender] Initializing...");

        ConfigManager.init();
        ModuleManager.init();

        // Register keybind
        guiKey = new KeyBinding("Open GUI", Keyboard.KEY_X, "RenderExtender");
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(guiKey);

        // Register event bus
        MinecraftForge.EVENT_BUS.register(this);

        System.out.println("[RenderExtender] Initialized successfully!");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Display.setTitle("Minecraft 1.8.9");
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        // GUI toggle
        if (guiKey.isPressed()) {
            ClickGUI.getInstance().toggle();
            return;
        }

        // Module keybinds - check directly via Keyboard events
        if (mc.currentScreen == null && Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if (key != 0) {
                ModuleManager.onKeyPress(key);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Empty - module ticks are handled in MixinMinecraft.runTick
    }

    public static boolean isGuiOpen() {
        return mc.currentScreen instanceof ClickGUI;
    }
}
