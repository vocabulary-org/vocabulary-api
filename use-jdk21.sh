#!/bin/sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
echo "Switched to $(java -version 2>&1 | head -1)"
