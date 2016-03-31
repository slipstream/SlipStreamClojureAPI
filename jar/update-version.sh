#!/bin/bash

version=$1
line="(def +version+ \"${version}\")"
sed -i '' "1s/.*/$line/" build.boot
