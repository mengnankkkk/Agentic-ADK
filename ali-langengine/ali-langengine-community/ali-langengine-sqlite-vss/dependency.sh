#!/bin/bash

# SQLite-VSS Dependency Check Script

echo "Checking dependencies for ali-langengine-sqlite-vss module..."

# Show dependency tree
mvn dependency:tree

echo ""
echo "Dependency analysis:"
mvn dependency:analyze

echo ""
echo "Checking for dependency conflicts:"
mvn dependency:analyze-duplicate

echo "Dependency check completed!"
