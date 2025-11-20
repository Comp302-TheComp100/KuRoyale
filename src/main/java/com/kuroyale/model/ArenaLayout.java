package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;
import java.awt.Point; // Using Point for simple coordinates

/** Represents the persistent configuration of an arena.
 * Information Expert: Knows the layout configuration (bridges, towers, etc.).*/
public class ArenaLayout {
    private String name;
    private List<Point> bridgePositions;
    // Add other customizable elements here if needed (e.g., obstacles)

    public ArenaLayout(String name) {
        this.name = name;
        this.bridgePositions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Point> getBridgePositions() {
        return bridgePositions;
    }

    public void setBridgePositions(List<Point> bridgePositions) {
        this.bridgePositions = bridgePositions;
    }

    public void addBridgePosition(int x, int y) {
        this.bridgePositions.add(new Point(x, y));
    }
}
