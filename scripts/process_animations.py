"""
Script to process PNG animation files:
1. Crop each image to its minimum bounding rectangle (removing transparent padding)
2. Normalize images per-troop to equal dimensions (max dimensions found within that troop)
"""

import os
from pathlib import Path
from PIL import Image
import argparse
from collections import defaultdict


def get_content_bounds(img):
    """
    Get the bounding box of non-transparent pixels.
    Returns (left, top, right, bottom) or None if image is fully transparent.
    """
    # Ensure image has alpha channel
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
    
    # Get the alpha channel
    alpha = img.split()[3]
    
    # Get bounding box of non-zero (non-transparent) pixels
    bbox = alpha.getbbox()
    return bbox


def crop_to_content(img):
    """
    Crop image to the minimum bounding rectangle containing all non-transparent pixels.
    """
    bbox = get_content_bounds(img)
    if bbox:
        return img.crop(bbox), bbox
    return img, None


def get_troop_name(png_path, troops_dir):
    """
    Extract the troop name from a PNG path.
    e.g., 'troops/knight/player/attack/000.png' -> 'knight'
    """
    rel_path = Path(png_path).relative_to(troops_dir)
    parts = rel_path.parts
    if len(parts) >= 1:
        return parts[0]  # First component is the troop name
    return None


def find_all_pngs_grouped_by_troop(troops_dir):
    """
    Find all PNG files and group them by troop name.
    Returns dict: {troop_name: [list of png paths]}
    """
    troops_by_name = defaultdict(list)
    
    for root, dirs, files in os.walk(troops_dir):
        for file in files:
            if file.lower().endswith('.png'):
                png_path = os.path.join(root, file)
                troop_name = get_troop_name(png_path, troops_dir)
                if troop_name:
                    troops_by_name[troop_name].append(png_path)
    
    return dict(troops_by_name)


def calculate_max_dimensions(png_files):
    """
    Calculate the maximum dimensions needed after cropping all images.
    Returns (max_width, max_height)
    """
    max_width = 0
    max_height = 0
    
    for png_path in png_files:
        try:
            with Image.open(png_path) as img:
                if img.mode != 'RGBA':
                    img = img.convert('RGBA')
                
                bbox = get_content_bounds(img)
                if bbox:
                    width = bbox[2] - bbox[0]
                    height = bbox[3] - bbox[1]
                    max_width = max(max_width, width)
                    max_height = max(max_height, height)
        except Exception as e:
            print(f"Error processing {png_path}: {e}")
    
    return max_width, max_height


def process_image(png_path, target_width, target_height):
    """
    Process a single image:
    1. Crop to content bounds
    2. Center on a canvas of target_width x target_height
    """
    with Image.open(png_path) as img:
        if img.mode != 'RGBA':
            img = img.convert('RGBA')
        
        # Crop to content
        cropped_img, bbox = crop_to_content(img)
        
        if bbox is None:
            # Fully transparent image - create empty canvas
            new_img = Image.new('RGBA', (target_width, target_height), (0, 0, 0, 0))
        else:
            # Create new canvas with target dimensions
            new_img = Image.new('RGBA', (target_width, target_height), (0, 0, 0, 0))
            
            # Calculate position to center the cropped content
            cropped_width, cropped_height = cropped_img.size
            x_offset = (target_width - cropped_width) // 2
            y_offset = (target_height - cropped_height) // 2
            
            # Paste cropped content centered on new canvas
            new_img.paste(cropped_img, (x_offset, y_offset))
        
        # Save back to same file
        new_img.save(png_path, 'PNG')


def main():
    parser = argparse.ArgumentParser(
        description='Process PNG animation files: crop backgrounds and normalize sizes per-troop'
    )
    parser.add_argument(
        'directory',
        nargs='?',
        default=None,
        help='Directory to process (default: animations/troops folder in resources)'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be done without modifying files'
    )
    
    args = parser.parse_args()
    
    # Determine the target directory
    if args.directory:
        target_dir = Path(args.directory)
    else:
        # Default to animations/troops folder relative to this script
        script_dir = Path(__file__).parent.parent
        target_dir = script_dir / 'src' / 'main' / 'resources' / 'images' / 'animations' / 'troops'
    
    if not target_dir.exists():
        print(f"Error: Directory does not exist: {target_dir}")
        return 1
    
    print(f"Processing PNG files in: {target_dir}")
    
    # Find all PNG files grouped by troop
    troops_dict = find_all_pngs_grouped_by_troop(target_dir)
    total_files = sum(len(files) for files in troops_dict.values())
    
    print(f"Found {len(troops_dict)} troop types with {total_files} total PNG files")
    
    if not troops_dict:
        print("No PNG files found.")
        return 0
    
    # Calculate max dimensions per troop
    print("\nPass 1: Calculating maximum dimensions per troop...")
    troop_dimensions = {}
    for troop_name, png_files in troops_dict.items():
        max_width, max_height = calculate_max_dimensions(png_files)
        troop_dimensions[troop_name] = (max_width, max_height)
        print(f"  {troop_name}: {max_width} x {max_height} ({len(png_files)} files)")
    
    if args.dry_run:
        print("\n[DRY RUN] Would process files with the dimensions shown above.")
        return 0
    
    # Process all images per troop
    print("\nPass 2: Processing images per troop...")
    total_processed = 0
    total_errors = 0
    
    for troop_name, png_files in troops_dict.items():
        max_width, max_height = troop_dimensions[troop_name]
        print(f"  Processing {troop_name} ({len(png_files)} files -> {max_width}x{max_height})...")
        
        for png_path in png_files:
            try:
                process_image(png_path, max_width, max_height)
                total_processed += 1
            except Exception as e:
                print(f"    Error processing {png_path}: {e}")
                total_errors += 1
    
    print(f"\nDone! Processed {total_processed} files, {total_errors} errors.")
    print("Each troop type now has its own normalized dimensions.")
    
    return 0


if __name__ == '__main__':
    exit(main())
