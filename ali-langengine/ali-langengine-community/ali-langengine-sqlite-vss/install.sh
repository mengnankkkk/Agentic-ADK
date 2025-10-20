#!/bin/bash

# SQLite-VSS Installation Script

echo "Installing ali-langengine-sqlite-vss dependencies and module..."

# Install the module
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "Installation successful!"
    echo "Module ali-langengine-sqlite-vss has been installed to local Maven repository"
else
    echo "Installation failed!"
    exit 1
fi

echo "SQLite-VSS module installation completed!"
