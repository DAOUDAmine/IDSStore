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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.cdo.eresource.CDOBinaryResource;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.CDOTextResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.browser.BrowserNode.BrowserNodeType;

public class CDOClientFolderTest {

	private static final String REPOSITORY_NAME = "esdldrive";
	private CDONet4jSessionConfiguration configuration;
	private IConnector connector;
	private IManagedContainer container;

	public void setUp() throws Exception {
		// Enable logging and tracing
	    OMPlatform.INSTANCE.setDebugging(true);
	    OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
	    OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

	    container = ContainerUtil.createContainer();
	    Net4jUtil.prepareContainer(container); // Register Net4j factories
	    TCPUtil.prepareContainer(container); // Register TCP factories
	    CDONet4jUtil.prepareContainer(container); // Register CDO factories
	    container.activate();

	    connector = TCPUtil.getConnector(container, "localhost:2036");

	    configuration = CDONet4jUtil.createNet4jSessionConfiguration();
	    configuration.setConnector(connector);
	    configuration.setRepositoryName(REPOSITORY_NAME); //$NON-NLS-1$

	   
	  
	}
		



	public void test() throws ConcurrentAccessException, CommitException {
		System.out.println("Starting transaction");

		 // Open session
	    CDOSession session = configuration.openNet4jSession();
	    session.getPackageRegistry().putEPackage(EsdlPackage.eINSTANCE);

	    // Open transaction
	    CDOTransaction transaction = session.openTransaction();

	    // Get or create resource
	    CDOResource resource = transaction.getOrCreateResource("/folder2/test.esdl"); //$NON-NLS-1$

	    // Work with the resource and commit the transaction
	    EnergySystem energySystem = EsdlFactory.eINSTANCE.createEnergySystem();
	    energySystem.setName("Test Energy System 2");
	    energySystem.setDescription("now = " + System.currentTimeMillis());
	    
	    resource.getContents().add(energySystem);
	    transaction.commit();

	    // Cleanup
	    session.close();
	    
		System.out.println("Stopping transaction");

	}
	
	public void getResource() {
		System.out.println("Read");
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();// Open a CDO transaction
		CDOResource resource = transaction.getResource("/folder2/test.esdl");// Create a new EMF resource
		EObject eObject = resource.getContents().get(0);
		System.out.println("Read: " + eObject);
		transaction.close();
		session.close();
	}

	public void afterTest() {
		connector.close();
	    container.deactivate();
	}
	
	
	public List<BrowserNode> getFolderContents(String path) {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		EList<CDOResourceNode> nodes;
		if (path.equals("/") || path.equals("")) {
			nodes = transaction.getRootResource().getContents()
					.stream().filter(CDOResourceNode.class::isInstance)
					.map(CDOResourceNode.class::cast)
					.collect(Collectors.toCollection(BasicEList::new));
		} else {		
			CDOResourceFolder resourceNode = transaction.getResourceFolder(path);
			nodes = resourceNode.getNodes();
		}
		ArrayList<BrowserNode> list = new ArrayList<>();
		for (CDOResourceNode node : nodes) {
			BrowserNode browserNode = new BrowserNode(node.getName(), node.getPath());
			if (node instanceof CDOResourceFolder) {
				CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) node;
				browserNode.setType(BrowserNodeType.FOLDER);
				if (cdoResourceFolder.getNodes().size() > 0) {
					browserNode.setHasSubNodes(true);
				}
			} else if (node instanceof CDOBinaryResource) {
				browserNode.setType(BrowserNodeType.BINARY);
			} else if (node instanceof CDOTextResource) {
				browserNode.setType(BrowserNodeType.TEXT);
			} else if (node instanceof CDOResource) {
				browserNode.setType(BrowserNodeType.ESDL_FILE);				
			}
			list.add(browserNode);
			
		}
		transaction.close();
		session.close();
		return list;
	}
	
	public static void main(String[] args) throws Exception {
		CDOClientFolderTest cdoClientTCPTest = new CDOClientFolderTest();
		cdoClientTCPTest.setUp();
		//cdoClientTCPTest.test();
		//cdoClientTCPTest.getResource();
		System.out.println(cdoClientTCPTest.getFolderContents("/"));
		cdoClientTCPTest.afterTest();
	}

	

}
