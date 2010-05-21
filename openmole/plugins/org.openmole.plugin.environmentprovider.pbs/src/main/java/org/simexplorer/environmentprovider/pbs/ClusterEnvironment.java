package org.openmole.environmentprovider.pbs; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.geclipse.GEclipseExecutionEnvironment;
import org.openmole.geclipse.GEclipseLimitedAccessJobService;
import org.openmole.geclipse.GEclipseURIStorage;
import org.openmole.geclipse.IGEclipseExecutionEnvironment;
import org.openmole.geclipse.IGEclipseJobService;
import org.openmole.geclipse.IGEclipseStorage;
import org.openmole.jobupdater.IWorkloadManagmentStrategie;

import eu.geclipse.batch.ISSHConnectionInfo;
import eu.geclipse.core.model.IVirtualOrganization;
import eu.geclipse.core.reporting.ProblemException;
import eu.geclipse.pbs.resource.PBSJobService;
import eu.geclipse.pbs.resource.SSHConnectionInfo;

public class ClusterEnvironment extends GEclipseExecutionEnvironment {
	
	final public static String environment = "pbs";
	
	transient ISSHConnectionInfo connectionInfo;
	transient GEclipseURIStorage storage;
	
	String address;
	String storageAddress;
	String user;
	String queue;

	IWorkloadManagmentStrategie strategy = new NoResubmit();
	
	transient PBSJobService service;

	//public ClusterEnvironment(String address,String storageAddress, String description) {
	//	this(address, null, storageAddress, description);
	//}
	
	public ClusterEnvironment(String address, String user, String storageAddress,String queue,  String description) {
		super(environment, description);
		this.address = address;
		this.storageAddress = storageAddress;
		this.user = user;		
	}


	/*@Override
	public URI getApplicationURI() {
		return application;
	}*/

	@Override
	public List<IGEclipseJobService> getJobServices() throws InternalProcessingError {
		List<IGEclipseJobService> ret = new LinkedList<IGEclipseJobService>();
		ret.add(new GEclipseLimitedAccessJobService<IGEclipseExecutionEnvironment>(getService(),5, this));
		return ret;
	}

	@Override
	public String getName() {
		return connectionInfo.getHostname();
	}

	@Override
	public List<IGEclipseStorage> getStorages() throws InternalProcessingError {
		List<IGEclipseStorage> ret = new LinkedList<IGEclipseStorage>();
		ret.add(storage);
		return ret;
	}

	@Override
	public IVirtualOrganization getVo() throws InternalProcessingError {
		return null;
	}

	@Override
	public IWorkloadManagmentStrategie getWorkloadManagmentStrategie() {
		return strategy;
	}


	PBSJobService getService() throws InternalProcessingError {
		if(service == null)
			try {
				service = new PBSJobService(connectionInfo, queue);
			//	service.setQueue(queue);
			} catch (ProblemException e) {
				throw new InternalProcessingError(e);
			}
				
		return service;
	}


	@Override
	public List<IGEclipseJobService> allJobServices() throws InternalProcessingError {
		List<IGEclipseJobService> ret = new LinkedList<IGEclipseJobService>();
		
		ret.add(new GEclipseLimitedAccessJobService(getService(), 5, this));
		
		return ret;
	}

	@Override
	public List<IGEclipseStorage> allStorages() throws InternalProcessingError {
		List<IGEclipseStorage> ret = new LinkedList<IGEclipseStorage>();
		ret.add(storage);
		return ret;
	}


	@Override
	public void initAuthentication() throws UserBadDataError, InternalProcessingError {
		if(user == null)
			user= Preferences.userRoot().get("SSHUserName", "");
		
		connectionInfo = new SSHConnectionInfo (address,"", 22, user);
		storage = new GEclipseURIStorage(this, URI.create(storageAddress));

	}
	
	public static void setPref() throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Would you like to enter the preferences: ");
		String ans = in.readLine();

		if(ans.compareToIgnoreCase("yes") == 0) {

			System.out.println("Please enter your ssh user name: ");
			Preferences.userRoot().remove("SSHUserName");
			String certPath = in.readLine();
			Preferences.userRoot().put("SSHUserName", certPath);


		/*	System.out.println("Please enter your ssh password (if needed): ");
			Preferences.userRoot().remove("SSHPassword");
			String keyPath = in.readLine();
			if(!keyPath.isEmpty())
				Preferences.userRoot().put("SSHPassword", keyPath);*/
		}
	}
	
	

}
