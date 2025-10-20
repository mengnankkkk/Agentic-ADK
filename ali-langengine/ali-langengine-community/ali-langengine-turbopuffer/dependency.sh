#!/bin/bash

# Turbopuffer module dependency installation script

echo "Installing ali-langengine-turbopuffer dependencies..."

mvn dependency:resolve

if [ $? -eq 0 ]; then
    echo "Dependencies installed successfully!"
else
    echo "Failed to install dependencies!"
    exit 1
fi
