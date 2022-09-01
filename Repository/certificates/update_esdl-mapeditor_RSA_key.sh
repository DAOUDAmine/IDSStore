KEYSTORE_FILE=public.p12
CERTIFICATE_FILE=esdl-mapeditor.pem
CERT=$( curl -s http://localhost:8080/auth/realms/esdl-mapeditor/protocol/openid-connect/certs | jq -r '.keys[0].x5c[0]'  )
echo "-----BEGIN CERTIFICATE-----" > $CERTIFICATE_FILE
echo $CERT >> $CERTIFICATE_FILE
echo "-----END CERTIFICATE-----" >> $CERTIFICATE_FILE
cat $CERTIFICATE_FILE
echo Importing this certificate, enter keystore password twice
keytool -delete -alias esdl-mapeditor -keystore $KEYSTORE_FILE
keytool -importcert -alias esdl-mapeditor -file $CERTIFICATE_FILE -trustcacerts -keystore $KEYSTORE_FILE
