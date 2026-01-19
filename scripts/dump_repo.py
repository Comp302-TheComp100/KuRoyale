import os

def dump_repo(root_dir, output_file):
    ignore_dirs = {'.git', '.mvn', '.vscode', 'target', '__pycache__', 'docs', 'documentation'}
    ignore_files = {'.gitignore', 'mvnw', 'mvnw.cmd', 'pom.xml', 'build.log', 'error.log', 'test_output.log', 'repo_content.txt', 'dump_repo.py'}
    
    with open(output_file, 'w', encoding='utf-8') as f:
        for root, dirs, files in os.walk(root_dir):
            # Prune ignored directories
            dirs[:] = [d for d in dirs if d not in ignore_dirs]
            
            for file in files:
                if file in ignore_files:
                    continue
                
                # Only include code-like files for analysis
                if not any(file.endswith(ext) for ext in ['.java', '.py', '.xml', '.properties', '.sh', '.bat', '.ps1']):
                    continue
                
                file_path = os.path.join(root, file)
                relative_path = os.path.relpath(file_path, root_dir)
                
                f.write(f"\n{'='*80}\n")
                f.write(f"FILE: {relative_path}\n")
                f.write(f"{'='*80}\n\n")
                
                try:
                    with open(file_path, 'r', encoding='utf-8') as content_file:
                        f.write(content_file.read())
                except Exception as e:
                    f.write(f"Error reading file: {e}\n")
                
                f.write("\n")

if __name__ == "__main__":
    current_dir = os.getcwd()
    output_filename = "repo_content.txt"
    print(f"Dumping repository content from {current_dir} to {output_filename}...")
    dump_repo(current_dir, output_filename)
    print("Done.")
