#!/bin/bash

# Turbopuffer module installation script

echo "Installing ali-langengine-turbopuffer..."

mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "Installation completed successfully!"
else
    echo "Installation failed!"
    exit 1
fi
