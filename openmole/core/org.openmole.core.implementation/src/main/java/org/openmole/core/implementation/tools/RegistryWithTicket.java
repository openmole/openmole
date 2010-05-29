/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.tools;

import org.openmole.commons.tools.structure.Registry;
import org.openmole.commons.tools.structure.IRegistry;
import org.openmole.core.workflow.model.tools.IRegistryWithTicket;
import java.util.Map;
import java.util.WeakHashMap;

import org.openmole.core.workflow.model.job.ITicket;

public class RegistryWithTicket<K,V>  implements IRegistryWithTicket<K,V> {

	//private static IJobRegistry instance = new JobRegistryWithTicket();

	Map<ITicket, IRegistry<K,V>> registries = new WeakHashMap<ITicket, IRegistry<K,V>>();
//	JobRegistry global = new JobRegistry();
//	Map<ITicket, JobRegistry> explorationJobs = new WeakHashMap<ITicket,JobRegistry>();
	
	/*public static IJobRegistry GetInstance() {
		return instance;
	}
	*/
	IRegistry<K,V> getRegistry(ITicket ticket) {
		IRegistry<K,V> ret = registries.get(ticket);
		if(ret == null) {
			//System.out.println("new context reg " + ticket.hashCode());
			ret = new Registry<K,V>();
			registries.put(ticket, ret);
		}
		return ret;
	}
	
	@Override
	public synchronized V consult(K key,
			ITicket ticket) {
		return getRegistry(ticket).consult(key);
	}


	/*public synchronized IJob consumn(ITransition<?, ?> transition, ITicket ticket) {
		return getJobRegistry(ticket).consumn(transition);
	}*/


	/*public synchronized Collection<IJob> consumnAll(ITransition<?, ?> transition,
			ITicket ticket) {
		return getJobRegistry(ticket).consumnAll(transition);
	}*/


	@Override
	public synchronized boolean isRegistredFor(K key, ITicket ticket) {
		return getRegistry(ticket).isRegistredFor(key);
	}


	/*
	public synchronized void registerAllJobs(ITransition<?, ?> transition,
			Collection<? extends IJob> jobs, ITicket ticket) {
		getJobRegistry(ticket).registerAllJobs(transition, jobs);
	}*/


	@Override
	public synchronized void register(K key, ITicket ticket, V object) {
		getRegistry(ticket).register(key, object);
	}

	@Override
	public synchronized void removeFromRegistry(K key, ITicket ticket) {
		getRegistry(ticket).removeFromRegistry(key);
	}

	
}
