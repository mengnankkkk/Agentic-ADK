#!/bin/bash

# SQLite-VSS Deployment Script

echo "Building and deploying ali-langengine-sqlite-vss module..."

# Build the project
mvn clean compile package -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful!"
else
    echo "Build failed!"
    exit 1
fi

# Install to local repository
mvn install -DskipTests

if [ $? -eq 0 ]; then
    echo "Installation to local repository successful!"
else
    echo "Installation failed!"
    exit 1
fi

echo "SQLite-VSS module deployment completed successfully!"
