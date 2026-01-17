#!/bin/bash
# Run script that ensures clean build with correct Java version

# Initialize SDKMAN
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Navigate to project
cd "$(dirname "$0")"

# Clean and compile first (ensures correct Java version)
echo "Building game..."
rm -rf target
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Starting game..."
mvn javafx:run
