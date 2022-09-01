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

package nl.tno.esdl.esdldrive.browser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.util.CommitException;

import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

import nl.tno.esdl.esdldrive.browser.BrowserNode.BrowserNodeType;
import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.PermissionDeniedException;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;

/**
 *
 */
@Path("/browse")
@ApplicationScoped
public class BrowserController {
	
	
	private static final String ROOT_FOLDER_NAME = "ESDL Drive";

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	CDOManager cdoManager;
	
	private enum Operation {
		none,
		get_node,
		move_node,
		copy_node,
		delete_node,
		create_node,
		rename_node,
		get_content
	}
	
	private enum CreateType {
		folder,
		file
	}
	
    @GET
    @RolesAllowed("cdo_read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response browse(
    		@Context SecurityContext securityContext, 
    		@QueryParam("operation") String operation,
    		@QueryParam("id") String id,
    		@QueryParam("text") String newName,
    		@QueryParam("type") String type,
    		@QueryParam("parent") String parent)  {
        IdToken token = PropagationHelper.getIdToken();
        UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.NETWORK_AUTHENTICATION_REQUIRED).entity(e.getMessage()).build();
		}        //System.out.println("Token: " + token);
        //System.out.println("NEW User groups:" +token.getClaim("user_group"));
        
        Operation op = Operation.none;
        if (operation != null && !(operation.length() == 0)) {
        	op = Operation.valueOf(operation);
        } else {
        	return Response.serverError().status(Status.BAD_REQUEST).entity(generateError("operation parameter missing")).build();
        }
        
        if (id == null || id.isEmpty()) {
        	return Response.serverError().status(Status.BAD_REQUEST).entity(generateError("id parameter missing")).build();
        }
        
        JsonStructure jsonObject = null;
        switch(op) {
		case get_node:
			if (id.equals("#")) {
				
				JsonArray children = getFolderContents(userInfo, "/");
				
				JsonObject root = Json.createObjectBuilder().add("text", ROOT_FOLDER_NAME)
						.add("children", children)
						.add("id", "/")
						.add("icon", BrowserNodeType.FOLDER.getIconName())
						.add("type", BrowserNodeType.FOLDER.getIconName())
						.build();
				jsonObject = Json.createArrayBuilder().add(root).build();
			} else {
				jsonObject = getFolderContents(userInfo, id);
				
			}
			break;
		case get_content:
			try {
				Map<String, Object> contentSummary = cdoManager.getContentSummary(userInfo, id);
				JsonObjectBuilder contentBuilder = Json.createObjectBuilder();
				for (Entry<String, Object> entry: contentSummary.entrySet()) {
					convertToActualType(contentBuilder, entry.getKey(), entry.getValue());
				}
				jsonObject = contentBuilder.build();
				System.out.println(jsonObject);
			} catch (PermissionDeniedException e) {
				return Response.serverError().status(Status.FORBIDDEN).entity(generateError("Access denied")).build();
			}
			break;
		case create_node:
			String parentFolder = id;
			if (type == null || type.isEmpty()) {
	        	return Response.serverError().status(Status.BAD_REQUEST).entity(generateError("type parameter missing")).build();
	        }
			if (newName == null || newName.isEmpty()) {
	        	return Response.serverError().status(Status.BAD_REQUEST).entity(generateError("name of file or folder is missing")).build();
	        }
			if (type.equals("default")) { type = "folder"; }
			CreateType createType = CreateType.valueOf(type);
			switch (createType) {
			case file:
				break;
			case folder:
				String newPath = parentFolder + "/" + newName;
				if (newPath.startsWith("//")) {
					newPath = parentFolder + newName;
				}
				try {
					System.out.println("Creating folder:" + newPath);
					String newFolder = cdoManager.createFolder(userInfo, newPath);
					jsonObject = Json.createObjectBuilder().add("id", newFolder).build();
				} catch (CommitException e) {
					log.severe("Error in creating folder:" + e);
					return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(generateError("error creating folder")).build();
				} catch (PermissionDeniedException e) {
					return Response.serverError().status(Status.FORBIDDEN).entity(generateError("Access denied")).build();
				}
				// return d.id, id == new path
				break;
			}
			break;
		case rename_node:
			String folderPath = id;
			System.out.println(id);
			try {
				String renamedFolder = cdoManager.rename(userInfo, folderPath, newName);
				jsonObject = Json.createObjectBuilder().add("id", renamedFolder).build();
			} catch (CommitException e) {
				log.severe("Error in creating folder:" + e);
				return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(generateError("error renaming folder")).build();
			} catch (CDOResourceNodeNotFoundException e ) {
				return Response.serverError().status(Status.NOT_FOUND).entity(generateError("can't find folder")).build();
			} catch (PermissionDeniedException e) {
				return Response.serverError().status(Status.FORBIDDEN).entity(generateError("Access denied")).build();
			}
			break;
		case delete_node:
			try {
				cdoManager.deleteResource(userInfo, id);
				jsonObject = Json.createObjectBuilder().add("deleted", true).build();
			} catch (CDOResourceNodeNotFoundException e) {
				return Response.serverError().status(Status.NOT_FOUND).entity(generateError("can't find file or folder " + id)).build();
			} catch (CommitException e) {
				e.printStackTrace();
				return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(generateError("error renaming folder" + id)).build();
			} catch (IOException e) {
				e.printStackTrace();
				return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(generateError("error renaming folder" + id)).build();
			} catch (PermissionDeniedException e) {
				return Response.serverError().status(Status.FORBIDDEN).entity(generateError("Access denied")).build();
			}
			break;
		case copy_node:
			if (parent == null || parent.isEmpty()) {
				return Response.serverError().status(Status.BAD_REQUEST).entity(generateError("parent parameter missing")).build();
			}
			try {
				String newPath = cdoManager.copyNode(userInfo, id, parent);
				jsonObject = Json.createObjectBuilder().add("id", newPath).build();
			} catch (CommitException e1) {
				e1.printStackTrace();
				return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(generateError("error copying file " + id)).build();
			} catch (PermissionDeniedException e) {
				return Response.serverError().status(Status.FORBIDDEN).entity(generateError("Access denied")).build();
			}
			break;
		case move_node:
			if (parent == null || parent.isEmpty()) {
				return Response.serverError().status(Status.BAD_REQUEST).entity(generateError("parent parameter missing")).build();
			}
			try {
				String newPath = cdoManager.moveNode(userInfo, id, parent);
				jsonObject = Json.createObjectBuilder().add("id", newPath).build();
			} catch (IOException | CommitException e) {
				return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).entity(generateError("error moving node")).build();
			} catch (PermissionDeniedException e) {
				return Response.serverError().status(Status.FORBIDDEN).entity(generateError("Access denied")).build();
			}
			break;	
		default:
			String auth = "Hello " + token.getSubject() + " alias " + token.getClaim("email") + ", you are in group: " + token.getClaim("user_group");
	    	jsonObject = Json.createObjectBuilder().add("auth", auth).build();
			break;
        
        }
        
        /**
         * Future way, if microprofile JWT correctly works in liberty when using openIdConnectClient
         */
//        Principal user = securityContext.getUserPrincipal();
//        if (user instanceof JsonWebToken) {
//        	Set<String> groups = null;
//        	JsonWebToken jwt = (JsonWebToken) user;
//        	groups = jwt.getGroups();
//        	Object claim = jwt.getClaim("user_group");
//        	System.out.println("User group:" + claim);
//        	System.out.println("Groups: " + groups);
//        }
        
        System.out.println("Browser returning (" + op +"): " + jsonObject);
        return Response.accepted(jsonObject).build();
    }
    
    private void convertToActualType(JsonObjectBuilder contentBuilder, String key, Object value) {
    	if (value instanceof String) {
			contentBuilder.add(key, (String)value);
		} else if (value instanceof Integer) {
			contentBuilder.add(key, (Integer)value);
		} else if (value instanceof Boolean) {
			contentBuilder.add(key, (Boolean)value);
		} else if (value instanceof Float) {
			contentBuilder.add(key, (Float)value);
		} else if (value instanceof Long) {
			contentBuilder.add(key, (Long)value);
		} else if (value instanceof BigInteger) {
			contentBuilder.add(key, (BigInteger)value);
		} else if (value instanceof BigDecimal) {
			contentBuilder.add(key, (BigDecimal)value);
		} else {
			log.info("Ignoring " + key);
		}
		
	}

	private JsonObject generateError(String reason) {
    	return Json.createObjectBuilder().add("error", reason).build();
    }
    
    private JsonArray getFolderContents(UserInfo userInfo, String path) {
    	log.info("Get folder contents of "+ path);
    	List<BrowserNode> folderContents = cdoManager.getFolderContents(userInfo, path);
    	JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    	for (BrowserNode node : folderContents) {
    		JsonObject nodeAsJson = Json.createObjectBuilder()
    		.add("text", node.getName())
			.add("children", node.hasSubNodes())
			.add("id", node.getPath())
			.add("icon", node.getType().getIconName())
			.add("type", node.getType().getIconName())
			.add("writable", node.isWritable())
			.build();
			arrayBuilder.add(nodeAsJson);
		}
    	    	
    	return arrayBuilder.build();
    }
    
}
