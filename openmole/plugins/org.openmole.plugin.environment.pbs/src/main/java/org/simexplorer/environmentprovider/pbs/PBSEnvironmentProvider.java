package org.openmole.environmentprovider.pbs;

import java.util.HashMap;
import java.util.Map;

import org.openmole.core.execution.IExecutionEnvironmentProvider;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.geclipse.IGEclipseExecutionEnvironment;

public class PBSEnvironmentProvider implements IExecutionEnvironmentProvider{

	
	private static Map<String, IGEclipseExecutionEnvironment> clusters = new HashMap<String, IGEclipseExecutionEnvironment>();
	
	/** Description should match the pattern: ssh_adress;URI_of_the_store */
	@Override
	public IGEclipseExecutionEnvironment getEnvironment(String description)
			throws InternalProcessingError {
		
		if(getClusters().containsKey(description)) return getClusters().get(description);
		
		if(!description.matches(".+,.+")) {
			throw new InternalProcessingError("Wrong description ; should be URI_of_the_storage,ssh_adress(,queue)");
		}
		
		Integer pp = description.indexOf(',');
		
		String storeURI = description.substring(0, pp );
		String sshAddress = description.substring(pp + 1, description.length());
		
		String user = "";
		String host = sshAddress;

		Integer ar = sshAddress.indexOf('@');
		
		if(ar != -1) {
			user = sshAddress.substring(0, ar);
			host = sshAddress.substring(ar + 1 , sshAddress.length()); 
		}
		
		pp = host.indexOf(',');
		
		String queue = null;
		
		if(pp != -1) {
			queue = host.substring(pp + 1 , sshAddress.length());
			host = host.substring(0,pp);
		}
	//	System.out.println(storeURI);
		
		IGEclipseExecutionEnvironment cluster = new ClusterEnvironment(host, user, storeURI, queue ,  description);
		//System.out.println(host + " " + user +" " + storeURI);
		getClusters().put(description, cluster);
		
		return cluster;
	}

	static Map<String, IGEclipseExecutionEnvironment> getClusters() {
		return clusters;
	}

	
	
}
