#!/bin/bash

echo "Checking Infinity dependencies..."
mvn dependency:tree -Dverbose
