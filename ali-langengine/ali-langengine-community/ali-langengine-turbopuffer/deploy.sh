#!/bin/bash

# Turbopuffer module deployment script

echo "Deploying ali-langengine-turbopuffer..."

mvn clean deploy

if [ $? -eq 0 ]; then
    echo "Deployment completed successfully!"
else
    echo "Deployment failed!"
    exit 1
fi
