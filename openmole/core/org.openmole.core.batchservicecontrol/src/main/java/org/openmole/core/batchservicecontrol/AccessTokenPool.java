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
package org.openmole.core.batchservicecontrol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.model.execution.batch.IAccessToken;
import org.openmole.core.workflow.model.execution.batch.IAccessTokenPool;


public class AccessTokenPool implements IAccessTokenPool {

	BlockingDeque<IAccessToken> tokens = new LinkedBlockingDeque<IAccessToken>();
	Set<IAccessToken> taken = Collections.synchronizedSet(new HashSet<IAccessToken>());

	AtomicInteger load;

	public AccessTokenPool(int nbTokens) {
		for(int i = 0; i < nbTokens; i++) {
			tokens.add(new AccessToken());
		}
		load.addAndGet(-nbTokens);
	}

	@Override
	public IAccessToken waitAToken() throws InterruptedException {
		load.incrementAndGet();
		IAccessToken token;
		
		try {
			token = tokens.take();
		} catch (InterruptedException e) {
			load.decrementAndGet();
			throw e;
		} 

		taken.add(token);
		return token;
	}

	@Override
	public IAccessToken waitAToken(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
		load.incrementAndGet();
		IAccessToken ret;
		
		try {
			ret = tokens.poll(time, unit);
		} catch (InterruptedException e){
			load.decrementAndGet();
			throw e;
		} 
		if(ret == null) {
			load.decrementAndGet();
			throw new TimeoutException();
		}
		
		taken.add(ret);
		return ret;
	}

	@Override
	public void releaseToken(IAccessToken token) throws InternalProcessingError {
		if(token == null) throw new NullPointerException();

		if(!taken.remove(token)) {
			throw new InternalProcessingError("Trying to release a token that hasn't been taken.");
		}

		tokens.add(token);					
		load.decrementAndGet();
	}

	@Override
	public IAccessToken getAccessTokenInterruptly() {
		load.incrementAndGet();
		IAccessToken token = tokens.poll();
		if(token != null) {
			taken.add(token);
		} else load.decrementAndGet();
		return token;
	}

	@Override
	public int getLoad() {
		//if(load < 0 && tokens.isEmpty()) Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Error load is " + load + " and token pool is empty.");
		return load.get();
	}


}
