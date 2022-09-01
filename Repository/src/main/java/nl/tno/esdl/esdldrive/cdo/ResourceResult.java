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


package nl.tno.esdl.esdldrive.cdo;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.view.CDOView;

/**
 * @author werkmane
 *
 */
public class ResourceResult implements Closeable {
	private CDOResource cdoResource;
	private CDOView view;

	private final static Logger log = Logger.getLogger(ResourceResult.class.getName());
	
	public ResourceResult(CDOResource cdoResource, CDOView view) {
		this.setCDOResource(cdoResource);
		this.view = view;
	}
	
	public CDOView getView() {
		return view;
	}

	@Override
	public void close() throws IOException {
		//log.info("Closing view of " + cdoResource.getPath());
		//view.close(); // not necessary in with new cached sessions
	}


	public Date getLastModified() {
		return new Date(cdoResource.getTimeStamp());
	}

	public CDOResource getCDOResource() {
		return cdoResource;
	}



	public void setCDOResource(CDOResource cdoResource) {
		this.cdoResource = cdoResource;
	}
}
