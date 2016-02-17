#!/bin/bash
set -x
rfile=/opt/slipstream/client/sbin/slipstream.context
lfile=~/slipstream.context
scp root@${1?"Provide host IP."}:$rfile $lfile
