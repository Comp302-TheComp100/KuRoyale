#!/usr/bin/env python3
"""
Get troop sprite sizes from animations.
Reads the first frame of each troop's walk animation and outputs suggested sizes.
"""

from pathlib import Path
from PIL import Image

# Base path to animations
ANIMATIONS_PATH = Path(__file__).parent.parent / "src/main/resources/images/animations/troops"

# Scale factor: converts pixels to game tiles (adjust as needed)
# Higher value = smaller in-game size
PIXELS_PER_TILE = 64.0

def get_troop_sizes():
    """Read animation frames and calculate sizes for each troop."""
    if not ANIMATIONS_PATH.exists():
        print(f"Error: Animations path not found: {ANIMATIONS_PATH}")
        return
    
    results = []
    
    for troop_dir in sorted(ANIMATIONS_PATH.iterdir()):
        if not troop_dir.is_dir():
            continue
        
        troop_name = troop_dir.name
        
        # Try to find a walk animation first, then attack, then idle
        sample_frame = None
        for anim_type in ["walk", "attack", "idle"]:
            player_path = troop_dir / "player" / anim_type / "000.png"
            if player_path.exists():
                sample_frame = player_path
                break
        
        if not sample_frame:
            # Find any PNG
            pngs = list(troop_dir.rglob("*.png"))
            if pngs:
                sample_frame = pngs[0]
        
        if not sample_frame:
            print(f"Warning: No animation found for {troop_name}")
            continue
        
        try:
            img = Image.open(sample_frame)
            width, height = img.size
            
            # Use the maximum dimension for a circular hitbox
            max_dim = max(width, height)
            tile_size = max_dim / PIXELS_PER_TILE
            
            # Round to 2 decimal places
            tile_size = round(tile_size, 2)
            
            results.append({
                "name": troop_name,
                "width_px": width,
                "height_px": height,
                "tile_size": tile_size,
                "sample": sample_frame.name
            })
            
        except Exception as e:
            print(f"Error reading {sample_frame}: {e}")
    
    # Print results
    print("\n" + "=" * 70)
    print("TROOP SIZES FROM ANIMATIONS")
    print("=" * 70)
    print(f"{'Troop':<15} {'Width':<8} {'Height':<8} {'Max':<8} {'Tile Size':<10}")
    print("-" * 70)
    
    for r in results:
        print(f"{r['name']:<15} {r['width_px']:<8} {r['height_px']:<8} {max(r['width_px'], r['height_px']):<8} {r['tile_size']:<10}")
    
    print("\n" + "=" * 70)
    print("SUGGESTED CardFactory VALUES (width, height as last 2 args):")
    print("=" * 70)
    
    # Map folder names to Java method names
    name_map = {
        "archers": "Archers",
        "barbarians": "Barbarians", 
        "bomber": "Bomber",
        "giant": "Giant",
        "goblins": "Goblins",
        "hog_rider": "Hog Rider",
        "knight": "Knight",
        "mini_pekka": "Mini P.E.K.K.A",
        "minion_horde": "Minion Horde",
        "minions": "Minions",
        "musketeer": "Musketeer",
        "skeletons": "Skeletons",
        "spear_goblins": "Spear Goblins",
        "valkyrie": "Valkyrie",
        "wizard": "Wizard"
    }
    
    for r in results:
        card_name = name_map.get(r['name'], r['name'].replace('_', ' ').title())
        print(f'"{card_name}": {r["tile_size"]}, {r["tile_size"]}')


if __name__ == '__main__':
    get_troop_sizes()
