#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
echo Script location: $SCRIPT_DIR
JARNAME=$SCRIPT_DIR/ced.jar
MAIN=cnuphys.magfield.MagTests
VARGS="-Dsun.java2d.pmoffscreen=false -Xmx1024M -Xss512k"
cd $SCRIPT_DIR
java -jar $JARNAME $MAIN
