/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 *
 */
//@LoginConfig(authMethod = "MP-JWT")
@OpenAPIDefinition(
	components = 
		@Components(securitySchemes = {
				@SecurityScheme(
						securitySchemeName = "openId", 
						type = SecuritySchemeType.OPENIDCONNECT, /*bearerFormat = "Bearer: <JWT>",*/ 
						openIdConnectUrl = "https://idm.hesi.energy/auth/realms/esdl-mapeditor/.well-known/openid-configuration",
						description = "The API is protected by OpenIdConnect"
						),
				@SecurityScheme(
						securitySchemeName = "bearer",
						type = SecuritySchemeType.HTTP,
						bearerFormat = "JWT",
						description = "JWT-based security scheme",
						scheme = "bearer"
						
						)
				}
		), 
	security = {
		@SecurityRequirement(name = "openId", scopes = {"profile", "email", "user_group_path"}),
		@SecurityRequirement(name = "bearer")
	},
	info = @Info(
			contact = @Contact(email = "ewoud.werkman@tno.nl", name = "Ewoud Werkman", url = "http://www.tno.nl/"), 
			title = "ESDL Drive", 
			version="1.0",
			description = "This API provides ways to store and retrieve ESDL files from the ESDL Drive, a storage solution for ESDL files",
			license = @License(name = "Apache License 2.0", url="https://www.apache.org/licenses/LICENSE-2.0.html")) 
	
	)
@ApplicationPath("/store")
public class HubRestApplication extends Application {

}
