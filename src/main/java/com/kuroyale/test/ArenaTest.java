package com.kuroyale.test;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.TileType;
import com.kuroyale.service.ArenaService;
// import com.kuroyale.util.ServiceFactory;

public class ArenaTest {
    public static void main(String[] args) {
        System.out.println("Starting Arena Verification...");

        // Initialize ServiceFactory
        // ServiceFactory.initialize();
        // ArenaService arenaService = ServiceFactory.getInstance().getArenaService();

        // Manual instantiation to avoid dependencies
        ArenaService arenaService = new ArenaService(null);

        // Test 1: Create Default Layout
        System.out.println("Test 1: Create Default Layout");
        ArenaLayout layout = arenaService.createDefaultLayout();
        if (layout != null && "Default Arena".equals(layout.getName())) {
            System.out.println("PASS: Default layout created.");
        } else {
            System.out.println("FAIL: Default layout creation failed.");
        }

        // Test 2: Create Arena from Layout
        System.out.println("Test 2: Create Arena from Layout");
        Arena arena = arenaService.createArena(layout);
        if (arena != null) {
            System.out.println("PASS: Arena created.");
        } else {
            System.out.println("FAIL: Arena creation failed.");
        }

        // Test 3: Verify Grid and Bridges
        System.out.println("Test 3: Verify Grid and Bridges");
        boolean bridgesFound = false;
        if (arena != null) {
            // Check a known bridge position (3, 15)
            if (arena.getTile(3, 15).getType() == TileType.BRIDGE) {
                bridgesFound = true;
                System.out.println("PASS: Bridge found at (3, 15).");
            } else {
                System.out.println("FAIL: Bridge not found at (3, 15). Found: " + arena.getTile(3, 15).getType());
            }
        }

        // Test 4: Verify Bounds
        System.out.println("Test 4: Verify Bounds");
        if (arena.isValidPosition(0, 0) && !arena.isValidPosition(-1, 0) && !arena.isValidPosition(18, 32)) {
            System.out.println("PASS: Bounds check passed.");
        } else {
            System.out.println("FAIL: Bounds check failed.");
        }

        System.out.println("Verification Complete.");
    }
}
