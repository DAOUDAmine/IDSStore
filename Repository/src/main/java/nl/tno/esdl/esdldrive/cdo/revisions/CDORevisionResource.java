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

package nl.tno.esdl.esdldrive.cdo.revisions;

import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.emf.cdo.CDOObjectHistory;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.view.CDOView;

import com.ibm.websphere.security.openidconnect.PropagationHelper;

import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.HubPermission;
import nl.tno.esdl.esdldrive.cdo.ResourceResult;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;

@RequestScoped
@Path("/revision")
public class CDORevisionResource {

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	CDOManager cdoManager;

	@Path("/{resourceURL:.+}")
	@RolesAllowed("cdo_read")
	@GET
	public Response listHistory(@PathParam("resourceURL") String resourceURL) {
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
		
		
		String ret = "";
		log.info("listHistory() ResourceURL: " + resourceURL);
		try (ResourceResult resourceResult = cdoManager.getResource(userInfo, resourceURL)) {
			CDOResource cdoResource = resourceResult.getCDOResource();
			CDOView view = resourceResult.getView();
			CDOObjectHistory cdoHistory = cdoResource.cdoHistory();
			//cdoHistory.getManager().addCommitInfoHandler(new TextCommitInfoLog(System.out));
			cdoHistory.triggerLoad();
			while(cdoHistory.isLoading()) {
				Thread.sleep(10);
			}
			for (CDOCommitInfo info: cdoHistory.getElements()) {
				System.out.println(info);
				ret+= info + "<br/>";
				
			}
			ret+= "<br>---<br>";
			
			
			ret += "Revision version:" + cdoResource.cdoRevision().getVersion();
			
			
		} catch(Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
		}
		
		return Response.ok(ret).build();
	}
	
}
