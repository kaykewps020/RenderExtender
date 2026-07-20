package me.cheat.client.mixin;

import me.cheat.client.CheatClient;
import me.cheat.client.modules.Module;
import me.cheat.client.modules.ModuleManager;
import me.cheat.client.modules.movement.Blink;
import me.cheat.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Inject(method = "renderGameOverlay", at = @At("TAIL"))
    private void onRenderOverlayPost(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen instanceof GuiChat) return;

        // HUD Array List
        me.cheat.client.modules.Module hud = ModuleManager.getModule("HUD");
        if (hud != null && hud.isEnabled()) {
            drawArrayList(mc);
        }

        // Blink packet counter
        Blink blink = ModuleManager.getModule(Blink.class);
        if (blink != null && blink.isEnabled() && blink.isBlinking()) {
            drawBlinkCounter(mc, blink);
        }
    }

    private void drawArrayList(Minecraft mc) {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();

        // Collect enabled modules
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : ModuleManager.getModules()) {
            if (module.isEnabled()) {
                enabledModules.add(module);
            }
        }

        // Sort by name length (longest first) for CS:GO style
        enabledModules.sort(Comparator.comparingInt(
            (Module m) -> mc.fontRendererObj.getStringWidth(m.getName().toUpperCase())
        ).reversed());

        int y = 4;
        int index = 0;

        for (Module module : enabledModules) {
            String name = module.getName().toUpperCase();
            int textWidth = mc.fontRendererObj.getStringWidth(name);

            // Background bar
            int barX = screenWidth - textWidth - 6;
            RenderUtils.drawRect(barX - 2, y - 1, textWidth + 8, 12, 0xCC0F0A1E);

            // Left accent line (purple)
            RenderUtils.drawRect(barX - 2, y - 1, 2, 12, 0xFF7C3AED);

            // Text
            RenderUtils.drawText(name, barX + 1, y, 0xFFE2E8F0, false);

            // Settings info (e.g. CPS, range)
            String info = getModuleInfo(module);
            if (info != null) {
                int infoWidth = mc.fontRendererObj.getStringWidth(info);
                RenderUtils.drawRect(barX - infoWidth - 8, y - 1, infoWidth + 6, 12, 0xCC1A1233);
                RenderUtils.drawText(info, barX - infoWidth - 5, y, 0xFF94A3B8, false);
            }

            y += 13;
            index++;
        }
    }

    private String getModuleInfo(Module module) {
        Module.Setting range = module.getSetting("Range");
        if (range != null) return String.format("%.1f", range.getDouble());

        Module.Setting cps = module.getSetting("CPS");
        if (cps != null) return String.valueOf(cps.getInt());

        Module.Setting speed = module.getSetting("Speed");
        if (speed != null && module.getName().equals("Timer")) {
            return String.format("x%.1f", speed.getDouble());
        }

        Module.Setting mode = module.getSetting("Mode");
        if (mode != null) return mode.getMode();

        return null;
    }

    private void drawBlinkCounter(Minecraft mc, Blink blink) {
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() / 2 + 20;

        String text = "BLINK: " + blink.getPacketsStored() + " packets";
        int textWidth = mc.fontRendererObj.getStringWidth(text);

        RenderUtils.drawRect(centerX - textWidth / 2 - 4, y - 2, textWidth + 8, 12, 0xCC0F0A1E);
        RenderUtils.drawRect(centerX - textWidth / 2 - 4, y - 2, 2, 12, 0xFF7C3AED);
        RenderUtils.drawText(text, centerX - textWidth / 2, y, 0xFFA78BFA, true);
    }
}
