#!/bin/bash
# Build script for RenderExtender 1.8.9
echo "=== Building RenderExtender for 1.8.9 ==="

if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Install JDK 8."
    exit 1
fi

chmod +x gradlew 2>/dev/null || true

if [ "$1" == "clean" ]; then
    echo "[*] Cleaning..."
    ./gradlew clean
fi

echo "[*] Building..."
./gradlew build

if [ $? -eq 0 ]; then
    echo ""
    echo "=== BUILD SUCCESSFUL ==="
    echo "Output: build/libs/RenderExtender-2.0.jar"
    echo ""
    echo "To install on Pojav Launcher 1.8.9:"
    echo "1. Copy the .jar to:"
    echo "   /storage/emulated/0/games/PojavLauncher/.minecraft/mods/"
    echo "2. Launch with Forge 1.8.9 profile"
    echo "3. Press X in-game to open GUI"
    echo ""
    echo "Default keybinds:"
    echo "  X - Open GUI"
    echo "  R - Kill Aura"
    echo "  V - Scaffold"
else
    echo "=== BUILD FAILED ==="
    exit 1
fi
