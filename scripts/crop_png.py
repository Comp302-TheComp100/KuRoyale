#!/usr/bin/env python3
"""
PNG Cropper - Crops a PNG image to its minimum bounding box.
Removes transparent/empty borders while preserving all pixel data.

Usage:
    python crop_png.py <input.png> [output.png]
    python crop_png.py --batch <directory>
    
If output is not specified, it will overwrite the input file.
The --batch flag processes all PNG files in a directory recursively.
"""

import sys
import argparse
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Error: Pillow is required. Install with: pip install Pillow")
    sys.exit(1)


def get_bounding_box(image: Image.Image) -> tuple[int, int, int, int] | None:
    """
    Find the bounding box of non-transparent pixels.
    Returns (left, upper, right, lower) or None if image is fully transparent.
    """
    # Convert to RGBA if not already
    if image.mode != 'RGBA':
        image = image.convert('RGBA')
    
    # Get the alpha channel
    alpha = image.split()[3]
    
    # Get bounding box of non-zero alpha pixels
    bbox = alpha.getbbox()
    
    return bbox


def crop_png(input_path: str, output_path: str | None = None) -> bool:
    """
    Crop a PNG image to its minimum bounding box.
    
    Args:
        input_path: Path to input PNG file
        output_path: Path to output PNG file (defaults to input_path)
    
    Returns:
        True if successful, False otherwise
    """
    input_path = Path(input_path)
    output_path = Path(output_path) if output_path else input_path
    
    if not input_path.exists():
        print(f"Error: Input file not found: {input_path}")
        return False
    
    if not input_path.suffix.lower() == '.png':
        print(f"Warning: Input file may not be a PNG: {input_path}")
    
    try:
        # Open the image
        image = Image.open(input_path)
        original_size = image.size
        
        # Get bounding box
        bbox = get_bounding_box(image)
        
        if bbox is None:
            print(f"Warning: Image appears to be fully transparent: {input_path}")
            return False
        
        # Check if cropping is needed
        if bbox == (0, 0, original_size[0], original_size[1]):
            print(f"No cropping needed: {input_path}")
            if output_path != input_path:
                image.save(output_path, 'PNG')
            return True
        
        # Crop the image
        cropped = image.crop(bbox)
        new_size = cropped.size
        
        # Save the result
        cropped.save(output_path, 'PNG')
        
        # Report results
        original_pixels = original_size[0] * original_size[1]
        new_pixels = new_size[0] * new_size[1]
        reduction = (1 - new_pixels / original_pixels) * 100
        
        print(f"Cropped: {input_path}")
        print(f"  Original: {original_size[0]}x{original_size[1]} ({original_pixels:,} pixels)")
        print(f"  Cropped:  {new_size[0]}x{new_size[1]} ({new_pixels:,} pixels)")
        print(f"  Reduction: {reduction:.1f}%")
        print(f"  Saved to: {output_path}")
        
        return True
        
    except Exception as e:
        print(f"Error processing {input_path}: {e}")
        return False


def batch_crop(directory: str) -> tuple[int, int]:
    """
    Recursively crop all PNG files in a directory.
    
    Args:
        directory: Path to directory to process
    
    Returns:
        Tuple of (successful, failed) counts
    """
    directory = Path(directory)
    
    if not directory.exists():
        print(f"Error: Directory not found: {directory}")
        return 0, 0
    
    if not directory.is_dir():
        print(f"Error: Not a directory: {directory}")
        return 0, 0
    
    # Find all PNG files recursively
    png_files = list(directory.rglob('*.png'))
    
    if not png_files:
        print(f"No PNG files found in: {directory}")
        return 0, 0
    
    print(f"Found {len(png_files)} PNG files in {directory}")
    print("-" * 50)
    
    successful = 0
    failed = 0
    
    for png_file in png_files:
        if crop_png(str(png_file)):
            successful += 1
        else:
            failed += 1
        print()
    
    print("-" * 50)
    print(f"Batch complete: {successful} successful, {failed} failed")
    
    return successful, failed


def main():
    parser = argparse.ArgumentParser(
        description='Crop PNG images to their minimum bounding box.'
    )
    parser.add_argument(
        '--batch', '-b',
        action='store_true',
        help='Process all PNG files in the directory recursively'
    )
    parser.add_argument(
        'input',
        help='Input PNG file or directory (with --batch)'
    )
    parser.add_argument(
        'output',
        nargs='?',
        help='Output PNG file (optional, defaults to overwriting input)'
    )
    
    args = parser.parse_args()
    
    if args.batch:
        successful, failed = batch_crop(args.input)
        sys.exit(0 if failed == 0 else 1)
    else:
        success = crop_png(args.input, args.output)
        sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
