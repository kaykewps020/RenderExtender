package me.cheat.client.modules;

import me.cheat.client.CheatClient;
import me.cheat.client.utils.ConfigManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final Category category;
    private int keyBind;
    private boolean enabled;
    private final List<Setting> settings = new ArrayList<>();

    public Module(String name, Category category, int keyBind) {
        this.name = name;
        this.category = category;
        this.keyBind = keyBind;
        this.enabled = false;
    }

    public Module(String name, Category category) {
        this(name, category, 0);
    }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public int getKeyBind() { return keyBind; }
    public boolean isEnabled() { return enabled; }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
        ConfigManager.save("module." + name + ".bind", keyBind);
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            onEnable();
            CheatClient.instance.getClass(); // Ensure mod is loaded
        } else {
            onDisable();
        }
        ConfigManager.save("module." + name + ".enabled", enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            toggle();
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

    public void addSetting(Setting setting) {
        settings.add(setting);
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public Setting getSetting(String name) {
        for (Setting setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public static class Setting {
        private final String name;
        private final Type type;
        private Object value;
        private Object defaultValue;
        private double min;
        private double max;
        private double step;
        private List<String> options;

        public enum Type {
            BOOLEAN, INTEGER, DOUBLE, MODE, COLOR
        }

        public Setting(String name, boolean defaultValue) {
            this.name = name;
            this.type = Type.BOOLEAN;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        public Setting(String name, int defaultValue, int min, int max, int step) {
            this.name = name;
            this.type = Type.INTEGER;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public Setting(String name, double defaultValue, double min, double max, double step) {
            this.name = name;
            this.type = Type.DOUBLE;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public Setting(String name, String defaultValue, List<String> options) {
            this.name = name;
            this.type = Type.MODE;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
            this.options = options;
        }

        public String getName() { return name; }
        public Type getType() { return type; }

        @SuppressWarnings("unchecked")
        public <T> T getValue() { return (T) value; }

        public void setValue(Object value) { this.value = value; }

        public boolean getBoolean() { return (boolean) value; }
        public int getInt() { return ((Number) value).intValue(); }
        public double getDouble() { return ((Number) value).doubleValue(); }
        public String getMode() { return (String) value; }

        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getStep() { return step; }
        public List<String> getOptions() { return options; }
    }
}
