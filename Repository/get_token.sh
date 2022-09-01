#!/bin/bash

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: . $0 <username>"
  echo 1>&2 "  options:"
  echo 1>&2 "     -v (at the end)  be verbose"
  echo 1>&2 " The token will be stored in \$TOKEN environment variable "
  
  exit 1 
fi

#HOSTNAME=$1
#REALM_NAME=$2
USERNAME=$1
#CLIENT_ID=$4

HOSTNAME="idm.hesi.energy"
REALM_NAME="esdl-mapeditor"
CLIENT_ID="curl"


KEYCLOAK_URL=https://$HOSTNAME/auth/realms/$REALM_NAME/protocol/openid-connect/token



echo "Using authorization URL: $KEYCLOAK_URL"
echo " realm: $REALM_NAME"
echo " client-id: $CLIENT_ID"
echo " username: $USERNAME"
echo "Retrieving access token for Mondaine HUB requires your password"

echo -n "Password: " 
read -s PASSWORD
echo

WHOLE=$( curl -s "$KEYCLOAK_URL" \
 -H "Content-Type: application/x-www-form-urlencoded" \
 -d "username=$USERNAME" \
 -d "password=$PASSWORD" \
 -d 'grant_type=password' \
 -d "client_id=$CLIENT_ID" )

TKN=$( echo $WHOLE | jq -r .access_token )
VALID=$( echo $WHOLE | jq -r .expires_in )

echo Returned access token available in \$TOKEN env variable
echo The token is valid for $VALID seconds
unset TOKEN
if [[ $(echo $TKN) != 'null' ]]; then
	echo "Exporting token to \$TOKEN"
	export TOKEN=$TKN
fi
if [[ $2 == "-v" ]]; then
   echo Whole token: $WHOLE
   echo access token: $TOKEN
fi
