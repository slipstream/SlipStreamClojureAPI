#!/bin/bash


version=$1
echo "UPDATING BOOT VERSION: " ${version}

line="(def +version+ \"${version}\")"
sed -i '' "1s/.*/$line/" build.boot
