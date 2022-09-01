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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.ibm.websphere.security.openidconnect.PropagationHelper;

import esdl.EsdlPackage;
import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.HubPermission;
import nl.tno.esdl.esdldrive.cdo.PermissionDeniedException;
import nl.tno.esdl.esdldrive.cdo.ResourceMetadata;
import nl.tno.esdl.esdldrive.cdo.ResourceResult;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;

/**
 * @author werkmane
 */

@Path("/")
@RequestScoped
public class CDOServerResource {

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	CDOManager cdoManager;
	

	@GET
	@Path("/resource/{resourceURL:.+}")
	@Produces(MediaType.APPLICATION_XML)
	@RolesAllowed("cdo_read")
	//@Parameter(allowEmptyValue = false, in = ParameterIn.PATH, name = "resourceURL", description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces")
	public Response getResource(@PathParam("resourceURL") String resourceURL) {
		// String resourceURL = "ewoud/test1.esdl";
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.NETWORK_AUTHENTICATION_REQUIRED).entity(e.getMessage()).build();
		}
		
		HubPermission permission = AccessController.getPermission(resourceURL, userInfo.getGroups());
		if (!permission.isAccessible()) {
			return Response.serverError().status(Status.FORBIDDEN).entity("Access denied").build();
		}
		
		
		log.info("getResource() ResourceURL: " + resourceURL);
		// try to find it in the cache
		String cachedModel = cdoManager.getCachedModelXML(resourceURL);
		if (cachedModel != null) {
			return Response.ok(cachedModel).build();
		}
		try (ResourceResult resourceResult = cdoManager.getResource(userInfo, resourceURL)) {
			CDOResource cdoResource = resourceResult.getCDOResource();
			String xmlString = cdoManager.convertToXMLString(cdoResource);
			//log.info("Retrieved: " + xmlString);
			if (xmlString == null) {
				resourceResult.close();
				return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
			}
			cdoManager.setCache(resourceURL,xmlString, cdoResource);
			Date lastModified = resourceResult.getLastModified();
			resourceResult.close();
			return Response.ok(xmlString).lastModified(lastModified).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
		}
	}

	// update or create a new resource with resourceURL
	@PUT
	@Path("/resource/{resourceURL:.+}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM})
	@RolesAllowed("cdo_read")
	@Operation(description = "Retrieves a ESDL-file from the server")
	@Parameters(@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces", example = "folder/filename.esdl"))
	@APIResponses(@APIResponse(responseCode = "200", description = "Stores a resource in the repository using the supplied URI. Only response is a HTTP status code"))
	public Response putResource(
			@PathParam("resourceURL") 
			String resourceURL,
			@QueryParam("commitMessage") 
			String commitMessage,
			
			@RequestBody(description = "ESDL content in XML format", name = "stream", 
				content = {
					@Content(example = "<?xml version=\"1.0\" encoding=\"UTF-8\">\n<esdl:EnergySystem ...></esdl:EnergySystem", 
							schema = @Schema(required = true, format="XML String", description = "ESDL content"))
				}
			) 
			InputStream stream) {
		// String resourceURL = "ewoud/test1.esdl";
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.NETWORK_AUTHENTICATION_REQUIRED).entity(e.getMessage()).build();
		}
		HubPermission permission = AccessController.getPermission(resourceURL, userInfo.getGroups());
		System.out.println("putResource: " + resourceURL);
		if (permission.isWritable()) {
			
			// put in cache: and convert to EMF model
			try {
				System.out.println("Reading contents of " + resourceURL);
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = stream.read(buffer)) != -1) {
				    result.write(buffer, 0, length);
				}
				// StandardCharsets.UTF_8.name() > JDK 7
				String xmlString = result.toString(StandardCharsets.UTF_8.name());
				System.out.println("Storing in cache");
				cdoManager.setCache(resourceURL, xmlString, null);
				ByteArrayInputStream resourceInputStream = new ByteArrayInputStream(result.toByteArray()); 
				
				
				
				String generatedCommitMessage = null;
				if (commitMessage != null) {
					generatedCommitMessage = commitMessage;
				} else {
					generatedCommitMessage = "Initial upload";
				}
				
				// create hashmap to speed up reading
				HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP = new HashMap<String, EObject>();
				XMLResourceImpl resource = new XMLResourceImpl();
				resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
				resource.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP);
				resource.load(resourceInputStream, null);
				System.out.println("Done reading contents of " + resourceURL);
				EObject rootEObject = resource.getContents().get(0);
				CDOCommitInfo commitInfo = cdoManager.storeResource(userInfo, resourceURL, rootEObject, generatedCommitMessage);
				System.out.println("Saved " + resourceURL + ", commitInfo: " + commitInfo);
				long timeStamp = commitInfo.getTimeStamp();
				return Response.ok().lastModified(new Date(timeStamp)).build();
			} catch (Exception e) {
				log.severe("Error: " + e);
				e.printStackTrace();
				if (e instanceof PackageNotFoundException) {
					return Response.serverError().entity(
							"Input is not of type ESDL: XML namespaceURI is not defined or (xmlns:esdl) is different from "
									+ EsdlPackage.eNS_URI)
							.type(MediaType.TEXT_PLAIN).build();
				}
				System.out.println("Error saving resource " + resourceURL + ": " + e.getMessage());
				return Response.serverError().entity("Failed to store the resource " + resourceURL + ": " + e.getMessage())
						.type(MediaType.TEXT_PLAIN).build();
			}
		} else {
			System.out.println("No write access to " + permission.getPath());
			return Response.serverError().status(Status.FORBIDDEN).entity("Permission denied for " + resourceURL).build();
		}
	}

	/**
	 * HEAD -> ok if file exists
	 * 
	 * in GET, HEAD, HeaderField("last-modified") also content-length
	 * 
	 * in OPTIONS String allow = httpURLConnection.getHeaderField("Allow"); if PUT
	 * is in there, URI is marked read-write, otherwise read-only
	 * 
	 */
	@Operation(description = "Returns the status of the resource, e.g. if it exists and the last-modified state")
	@Parameter(allowEmptyValue = false, name = "resourceURL", description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces", example = "folder/filename.esdl")
	@APIResponses({
		@APIResponse(responseCode = "200", description = "Returns OK if the resource exists and is accessible by the user"),
		@APIResponse(responseCode = "403", description = "Returns Forbidden if the user is not allowed to access the resource"),
		@APIResponse(responseCode = "404", description = "Returns Not Found if resource is not found")
	})
	@HEAD
	@Path("/resource/{resourceURL:.+}")
	@RolesAllowed("cdo_read")
	public Response head(@PathParam("resourceURL") String resourceURL) {
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		log.info("getResource() ResourceURL: " + resourceURL);
		boolean resourceExists = false;
		try {
			UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
			
			HubPermission permission = AccessController.getPermission(resourceURL, userInfo.getGroups());
			if (!permission.isAccessible()) {
				return Response.serverError().status(Status.FORBIDDEN).entity("Access denied").build();
			}
			resourceExists = cdoManager.existResource(userInfo, resourceURL);
			if (resourceExists) {
				ResourceMetadata resourceMetaData = cdoManager.getResourceMetaData(userInfo, resourceURL);
				return Response.ok().lastModified(new Date(resourceMetaData.getTimeStamp())).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch (PermissionDeniedException | InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}

	
	/*
	@GET
	@Path("/store")
	@Produces(MediaType.TEXT_PLAIN)
	public Response storeEDSL() {
		UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		EnergySystem es = EsdlFactory.eINSTANCE.createEnergySystem();
		es.setName("New ES");
		es.setDescription("now = " + System.currentTimeMillis());

		try {
			cdoManager.storeResource(userInfo, "/ewoud/test1.esdl", es);
		} catch (Exception e) {
			log.severe("Error storing file in CDO repository " + e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity("Error storing resource " + e.getMessage()).build();
		}

		return Response.accepted("Success").build();
	}
	*/
	
	@GET
	@Path("/logout")
	@RolesAllowed("cdo_read")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(description = "Invalidates the user session")
	public Response logout(@Context HttpServletRequest servletRequest) {
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.NETWORK_AUTHENTICATION_REQUIRED).entity(e.getMessage()).build();
		}
		if (userInfo != null) {
			cdoManager.logoutSession(userInfo);
			if (servletRequest != null) {
				try {
					servletRequest.logout();
				} catch (ServletException e) {
					e.printStackTrace();
				}
			}
			return Response.ok().build();
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}

	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("cdo_read")
	public Response listResources() {
//		log.info("Token: " + callerPrincipal);
//		log.info("Username: " + callerPrincipal.getName());
//		log.info("Groups: " + callerPrincipal.getGroups());
//		log.info("Subject: " + callerPrincipal.getSubject());
//		log.info("Claim names: " + callerPrincipal.getClaimNames());
//		log.info("Roles: " + callerPrincipal.getClaim("roles"));
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.NETWORK_AUTHENTICATION_REQUIRED).entity(e.getMessage()).build();
		}
		if (userInfo != null) {
			log.info("List resources request by: " + userInfo);
			ArrayList<String> resources = cdoManager.listResources(userInfo);
			return Response.ok(resources).build();
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}
	
	
	
	
	/*
	@GET
	@Path("/listpath/{path}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listResourcesByPath(@PathParam("path") String path) {
		log.info("Path=" + path);
		IdToken token = PropagationHelper.getIdToken();
		UserInfo userInfo = AccessController.userInfoFromToken(token);
		if (userInfo != null) {
			ArrayList<String> resources = cdoManager.listPath(userInfo, path);
			return Response.ok(resources).build();
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}
	 */
}
