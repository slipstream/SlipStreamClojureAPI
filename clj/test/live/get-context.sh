#!/bin/bash
set -x
rfile=/opt/slipstream/client/sbin/slipstream.context
lfile=${2:-$rfile}
scp root@${1?"Provide host IP."}:$rfile $lfile
