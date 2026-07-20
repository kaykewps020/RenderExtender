package me.cheat.client.modules;

import me.cheat.client.modules.combat.*;
import me.cheat.client.modules.player.*;
import me.cheat.client.utils.ConfigManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        register(new KillAura());
        register(new Reach());
        register(new HitBox());
        register(new Scaffold());

        // Load saved states
        for (Module module : modules) {
            int bind = ConfigManager.load("module." + module.getName() + ".bind", module.getKeyBind());
            module.setKeyBind(bind);

            boolean enabled = ConfigManager.load("module." + module.getName() + ".enabled", false);
            if (enabled) {
                module.toggle();
            }
        }
    }

    private static void register(Module module) {
        modules.add(module);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public static List<Module> getModulesInCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public static Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (module.getClass() == clazz) {
                return (T) module;
            }
        }
        return null;
    }

    public static void onKeyPress(int key) {
        for (Module module : modules) {
            if (module.getKeyBind() == key) {
                module.toggle();
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Tick-based updates for modules
        }
    }
}
