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
package org.openmole.core.serializer.internal;

import java.util.NoSuchElementException;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

/**
 *
 * @author reuillon
 */
public class SerializerFactory implements ObjectPool{

    private ObjectPool bufferPool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {

        @Override
        public Object makeObject() {
            return new SerializerWithExtensibleClassListing();
        }

    });

    private static SerializerFactory instance = new SerializerFactory();

    private SerializerFactory() {
    }

    public static SerializerFactory GetInstance() {
        return instance;
    }

    @Override
    public void addObject() throws Exception, IllegalStateException,
            UnsupportedOperationException {
        bufferPool.addObject();
    }

    @Override
    public ISerializerWithExtensibleClassListing borrowObject() throws Exception, NoSuchElementException,
            IllegalStateException {
        return (ISerializerWithExtensibleClassListing) bufferPool.borrowObject();
    }

    @Override
    public void clear() throws Exception, UnsupportedOperationException {
        bufferPool.clear();
    }

    @Override
    public void close() throws Exception {
        bufferPool.close();
    }

    @Override
    public int getNumActive() throws UnsupportedOperationException {
        return bufferPool.getNumActive();
    }

    @Override
    public int getNumIdle() throws UnsupportedOperationException {
        return bufferPool.getNumIdle();
    }

    @Override
    public void invalidateObject(Object arg0) throws Exception {
        bufferPool.invalidateObject(arg0);
    }

    @Override
    public void returnObject(Object arg0) throws Exception {
        ISerializerWithExtensibleClassListing serial = (ISerializerWithExtensibleClassListing) arg0;
        serial.clean();
        bufferPool.returnObject(arg0);
    }

    @Override
    public void setFactory(PoolableObjectFactory arg0)
            throws IllegalStateException, UnsupportedOperationException {
        bufferPool.setFactory(arg0);
    }
}
