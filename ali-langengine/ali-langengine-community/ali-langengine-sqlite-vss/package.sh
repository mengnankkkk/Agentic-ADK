#!/bin/bash

# SQLite-VSS Package Script

echo "Packaging ali-langengine-sqlite-vss module..."

# Clean and package
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "Packaging successful!"
    echo "JAR files are available in target/ directory"
    ls -la target/*.jar
else
    echo "Packaging failed!"
    exit 1
fi

echo "SQLite-VSS module packaging completed!"
