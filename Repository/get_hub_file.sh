#!/bin/bash
HOSTNAME=localhost:9080/store/resource
if [ $# != 1 ]; then
  echo "Usage $0 <filename> - retrieve a ESDL file from the ESDLDrive"
  echo ""
  echo "This script assumes that the access token is available in \$TOKEN environment variable"
  echo "Example $0 /Projects/Project1/Filename.esdl"
fi
if [ -z $TOKEN ]; then
  echo "No valid TOKEN in environment variable. Use get_token.sh to get this token"
  exit 1
fi
FILENAME=$( echo $1 | sed 's/\(.*\)\/\(.*\)/\2/' )
curl -s "$HOSTNAME$1" -H "Authorization: Bearer $TOKEN" -o $FILENAME
echo Return value: $?
echo Saved $1 to $FILENAME
