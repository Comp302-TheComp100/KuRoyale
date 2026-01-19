import os
import sys

def generate_tree(start_path, output_file="java_src_architecture.txt"):
    """
    Scans ONLY the src/java directory (or similar variants) for .java files
    and exports the tree to a text file.
    """
    # 1. Define standard Java source paths to look for
    possible_src_paths = [
        os.path.join(start_path, "src", "main", "java", "com", "kuroyale"), # Standard Maven/Gradle
        os.path.join(start_path, "src", "java"),         # Older standard
        os.path.join(start_path, "src")                  # Generic src
    ]

    # 2. Determine the best root directory to start scanning
    target_path = start_path # Default to current if no src found
    for p in possible_src_paths:
        if os.path.exists(p):
            target_path = p
            break
            
    # 3. Folders to ignore (We usually don't want tests or resources in a pure code view)
    ignore_dirs = {'test', 'resources', 'Tests', 'fixtures'} 

    with open(output_file, 'w', encoding='utf-8') as f:
        
        def log(text):
            print(text)
            f.write(text + "\n")

        abs_target = os.path.abspath(target_path)
        log(f"Scanning Java Source at: {abs_target}")
        log("=" * 60)

        for root, dirs, files in os.walk(target_path):
            # Filter out ignored directories
            dirs[:] = [d for d in dirs if d not in ignore_dirs]
            
            # Filter for Java files only
            java_files = [file for file in files if file.endswith(".java")]
            
            # Calculate indentation level based on the target path
            rel_path = os.path.relpath(root, target_path)
            if rel_path == ".":
                level = 0
            else:
                level = rel_path.count(os.sep) + 1
                
            indent = ' ' * 4 * level
            
            # Print the folder name
            folder_name = os.path.basename(root)
            # Only skip printing the root folder name if we are inside it to avoid redundancy
            # (Optional visual preference)
            log(f"{indent}|-- {folder_name}/")
            
            # Print the Java files
            sub_indent = ' ' * 4 * (level + 1)
            for java_file in java_files:
                log(f"{sub_indent}|-- {java_file}")

    print("-" * 60)
    print(f"✅ Java architecture saved to: {os.path.abspath(output_file)}")

if __name__ == "__main__":
    # Use current directory if no argument provided
    path = sys.argv[1] if len(sys.argv) > 1 else "."
    
    if os.path.exists(path):
        generate_tree(path)
    else:
        print("Invalid path provided.")