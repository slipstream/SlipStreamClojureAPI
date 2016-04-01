#!/bin/bash

version=$1
echo "UPDATING BOOT VERSION: " ${version}

line="(def +version+ \"${version}\")"
sed -i.bak -e "1 s/.*/$line/" build.boot
rm -f *.bak
