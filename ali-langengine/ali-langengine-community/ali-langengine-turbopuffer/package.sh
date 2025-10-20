#!/bin/bash

# Turbopuffer module packaging script

echo "Packaging ali-langengine-turbopuffer..."

mvn clean package

if [ $? -eq 0 ]; then
    echo "Packaging completed successfully!"
else
    echo "Packaging failed!"
    exit 1
fi
