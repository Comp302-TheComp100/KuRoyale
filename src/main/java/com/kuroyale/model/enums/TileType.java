package com.kuroyale.model.enums;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.logic.*;

//Enum representing the different types of tiles in the arena.
public enum TileType {
    GRASS,
    WATER,
    BRIDGE,
    ROAD,
    PRINCESS_TOWER_USER, // User's Princess tower
    PRINCESS_TOWER_COMPUTER, // Computer's Princess tower
    KING_TOWER_USER, // User's King tower
    KING_TOWER_COMPUTER // Computer's King tower
}
