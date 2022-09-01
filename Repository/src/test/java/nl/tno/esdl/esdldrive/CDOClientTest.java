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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.mem.MEMStoreUtil;
import org.eclipse.emf.cdo.server.net4j.CDONet4jServerUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.jvm.JVMUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.om.OMPlatform;

import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;

public class CDOClientTest {

	private static final String REPOSITORY_NAME = "esdldrive";
	private CDOSession session;
	private IConnector connector;

	public void setUp() throws Exception {

		// Turn on tracing
		OMPlatform.INSTANCE.setDebugging(true);

		// Prepare the standalone infra structure (not needed when running inside
		// Eclipse)
		IManagedContainer container = ContainerUtil.createContainer(); // Create a wiring container
		Net4jUtil.prepareContainer(container); // Prepare the Net4j kernel
		JVMUtil.prepareContainer(container); // Prepare the JVM transport
		CDONet4jServerUtil.prepareContainer(container); // Prepare the CDO server
		CDONet4jUtil.prepareContainer(container); // Prepare the CDO client
		container.activate();

		// Start the transport and create a repository
		JVMUtil.getAcceptor(container, "default"); // Start the JVM transport
		CDOServerUtil.addRepository(container, createRepository()); // Start a CDO repository

		connector = JVMUtil.getConnector(container, "default");
		session = openSession(connector);

	}

	public void test() {
		System.out.println("Starting transaction");

		try {
			CDOTransaction transaction = session.openTransaction();// Open a CDO transaction
			Resource resource = transaction.createResource("/ES1.esdl");// Create a new EMF resource

			// Work normally with the EMF resource
			EObject inputModel = getInputModel();
			System.out.println(inputModel);
			resource.getContents().add(inputModel);
			transaction.commit();
		} catch (CommitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Stopping transaction");

	}
	
	public void getResource() {
		System.out.println("Read");
		CDOTransaction transaction = session.openTransaction();// Open a CDO transaction
		CDOResource resource = transaction.getResource("/ES1.esdl");// Create a new EMF resource
		EObject eObject = resource.getContents().get(0);
		System.out.println(eObject);
		transaction.close();
	}

	public void afterTest() {
		session.close();
		connector.close();
	}

	private static IRepository createRepository() {
		Map<String, String> props = new HashMap<String, String>();
		return CDOServerUtil.createRepository(REPOSITORY_NAME, createStore(), props);
	}

	private static IStore createStore() {
		// You might want to create an IDBStore here instead if memory is an issue!
		return MEMStoreUtil.createMEMStore();
	}

	private static EObject getInputModel() {
		EsdlPackage.eINSTANCE.eClass();
		EnergySystem es = EsdlFactory.eINSTANCE.createEnergySystem();
		es.setName("Test" + new Random().nextInt());
		return es;
	}

	protected static CDOSession openSession(IConnector connector) {
		CDONet4jSessionConfiguration configuration = CDONet4jUtil.createNet4jSessionConfiguration();
		configuration.setConnector(connector);
		configuration.setRepositoryName(REPOSITORY_NAME);
		return configuration.openNet4jSession();
	}
	
	public static void main(String[] args) throws Exception {
		CDOClientTest cdoClientTest = new CDOClientTest();
		cdoClientTest.setUp();
		cdoClientTest.test();
		cdoClientTest.getResource();
		cdoClientTest.afterTest();
	}


}
