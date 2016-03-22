#!/bin/bash
set -x
rfile=/opt/slipstream/client/sbin/slipstream.context
lfile=${2:-~/slipstream.context}
scp root@${1?"Provide host IP."}:$rfile $lfile
