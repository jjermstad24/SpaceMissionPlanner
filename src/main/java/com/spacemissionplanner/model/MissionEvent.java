package com.spacemissionplanner.model;

public abstract class MissionEvent {
    protected String name;
    protected String colorHex = "#00FFFF";

    public MissionEvent(String name) {
        this.name = name;
    }

    public MissionEvent(String name, String colorHex) {
        this.name = name;
        this.colorHex = colorHex;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    @Override
    public abstract String toString();
}
