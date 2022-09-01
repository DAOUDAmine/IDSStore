#!/bin/bash
KEYSTORE_FILE=public.p12
openssl s_client -connect idm.hesi.energy:443 -servername idm.hesi.energy < /dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > idm.hesi.energy.pem
keytool -delete -alias idm-hesi-energy -keystore $KEYSTORE_FILE
keytool -importcert -alias idm-hesi-energy -file idm.hesi.energy.pem -trustcacerts -keystore $KEYSTORE_FILE


