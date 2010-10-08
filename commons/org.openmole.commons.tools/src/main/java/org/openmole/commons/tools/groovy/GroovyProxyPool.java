/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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


package org.openmole.commons.tools.groovy;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openmole.commons.exception.InternalProcessingError;

public class GroovyProxyPool {

    private ObjectPool bufferPool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
        @Override
        public Object makeObject() throws Exception {
            return new GroovyProxy();
        }
    });

    public void returnObject(IGroovyProxy o) throws InternalProcessingError {
        try {
            bufferPool.returnObject(o);
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
    }

    public void invalidateObject(IGroovyProxy o) throws Exception {
        bufferPool.invalidateObject(o);
    }

    public int getNumIdle() throws UnsupportedOperationException {
        return bufferPool.getNumIdle();
    }

    public int getNumActive() throws UnsupportedOperationException {
        return bufferPool.getNumActive();
    }

    public void close() throws Exception {
        bufferPool.close();
    }

    public void clear() throws Exception, UnsupportedOperationException {
        bufferPool.clear();
    }

    public IGroovyProxy borrowObject() throws InternalProcessingError {
        try {
            return (IGroovyProxy) bufferPool.borrowObject();
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
    }

    public void addObject() throws Exception, IllegalStateException, UnsupportedOperationException {
        bufferPool.addObject();
    }

 
}
