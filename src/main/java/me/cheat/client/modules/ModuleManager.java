package me.cheat.client.modules;

import me.cheat.client.modules.combat.*;
import me.cheat.client.modules.movement.*;
import me.cheat.client.modules.player.*;
import me.cheat.client.modules.render.*;
import me.cheat.client.utils.ConfigManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Combat
        register(new KillAura());
        register(new SilentAim());
        register(new Reach());
        register(new HitBox());

        // Movement
        register(new Velocity());
        register(new AutoSprint());
        register(new Timer());
        register(new Blink());

        // Player
        register(new Scaffold());
        register(new NoFall());
        register(new ChestStealer());
        register(new AutoArmor());

        // Render
        register(new HUD());
        register(new Zoom());
        register(new NoHurtCam());

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

    public static void tickModules() {
        if (Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
}
