package me.cheat.client.modules;

public enum Category {
    COMBAT("Combat"),
    PLAYER("Player"),
    RENDER("Render"),
    MOVEMENT("Movement"),
    MISC("Misc");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
