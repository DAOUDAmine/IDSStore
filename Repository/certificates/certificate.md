# Configuring OpenIDConnect client in Liberty for keycloak

Get the keycloak configuration, use the following url:
```
https://idm.hesi.energy/auth/realms/esdl-mapeditor/.well-known/openid-configuration
```


## Import the certificate
The keycloak SSL certificate must be added to the keystore, as Java SSL outgoing communication with keycloak needs the public key to be verified.
This keystore is generated at startup in `target/liberty/wlp/usr/servers/hub/resources/security`. At startup it also generates a self-signed certificate for localhost. 
The Dockerfile automatically copies the `public.p12` to the correct directory in the docker image. Therefore edit the public.p12 in the root directory of this project. Use `copy_keystore.sh` to copy this file for local development using `mvn liberty:dev`.

The filename of the keystore is specified in `server.xml`:

`<keyStore id="defaultKeyStore" location="public.p12" type="PKCS12" password="*****" />`

To download the public SSL certificate to certificates folder do:
```
 openssl s_client -connect idm.hesi.energy:443 </dev/null | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > idm.hesi.energy.pem
```
To delete an existing alias in the keystore:
```
keytool -delete -keystore public.p12 -alias idm-hesi-energy
```
To add the certificate to the keystore do:

```
keytool -importcert -file idm.hesi.energy.pem -alias idm-hesi-energy -keystore .-trustcacerts ../public.p12
```


For localhost deployments it is also necessary to add the esdl-mapeditor realm public key (see keycloak admin page to get it, or get it from [https://idm.hesi.energy/auth/realms/esdl-mapeditor/protocol/openid-connect/certs]() ). This is necessary to verify the JWT token. If you use `discoveryEndpointUrl` in server.xml, it is discovered automatically, but this doesn't work if the internal and external URL of keycloak is different.

`$ keytool -importcert -alias esdl-mapeditor -file esdl-mapeditor-public_key.pem -trustcacerts -keystore ../public.p12`


## Packaging
The generation of the executable jar file can be performed by issuing the following command

    mvn clean package

This will create an executable jar file **hub.jar** within the _target_ maven folder. This can be started by executing the following command

    java -jar target/hub.jar


### Liberty Dev Mode

During development, you can use Liberty's development mode (dev mode) to code while observing and testing your changes on the fly.
With the dev mode, you can code along and watch the change reflected in the running server right away; 
unit and integration tests are run on pressing Enter in the command terminal; you can attach a debugger to the running server at any time to step through your code.

    mvn liberty:dev



To launch the test page, open your browser at the following URL

    http://localhost:9080/index.html













### Open API

Exposes the information about your endpoints in the format of the OpenAPI v3 specification. Specification [here](https://microprofile.io/project/eclipse/microprofile-open-api)

The index page contains a link to the OpenAPI information of your endpoints.





