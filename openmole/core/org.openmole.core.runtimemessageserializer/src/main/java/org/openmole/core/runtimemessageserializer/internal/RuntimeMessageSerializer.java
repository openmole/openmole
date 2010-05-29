/*
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.runtimemessageserializer.internal;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.model.message.IExecutionMessage;
import org.openmole.core.workflow.model.message.IJobForRuntime;
import org.openmole.core.workflow.model.message.IRuntimeResult;


import org.openmole.core.runtimemessageserializer.IRuntimeMessageSerializer;


public class RuntimeMessageSerializer implements IRuntimeMessageSerializer {

    static final XStream xstream = new XStream();
  
    public IJobForRuntime loadJobForRuntime(String filename) throws IOException {
        return loadJobForRuntime(new File(filename));
    }

    @Override
    public IJobForRuntime loadJobForRuntime(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            return loadJobForRuntime(is);
        } finally {
            is.close();
        }
    }

    public IJobForRuntime loadJobForRuntime(InputStream inputStream) {
        IJobForRuntime application = (IJobForRuntime) xstream.fromXML(inputStream);
        return application;
    }

    @Override
    public IRuntimeResult loadJarRuntimeResult(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        IRuntimeResult res;
        try {
            res = loadRuntimeResult(is);
        } finally {
            is.close();
        }

        return res;
    }

    public IRuntimeResult loadRuntimeResult(InputStream inputStream) {
        IRuntimeResult result = (IRuntimeResult) xstream.fromXML(inputStream);
        return result;
    }

    @Override
    public void saveRuntimeResult(IRuntimeResult result, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            saveRuntimeResult(result, os);
        } finally {
            os.close();
        }
    }

    public void saveRuntimeResult(IRuntimeResult result, OutputStream outputStream) {
        xstream.toXML(result, outputStream);
    }

   


 
    @Override
    public Iterable<Class> saveJobForRuntime(IJobForRuntime job, File file) throws IOException, InternalProcessingError {
      /*  GroovyProject gp;
        synchronized (compilationCache) {
            gp = compilationCache.get(structures);

            if (gp == null) {
                //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,"Compile struct");

                gp = packager.compile(structures);
                compilationCache.put(structures, gp);
            }
        }
*/

        OutputStream zos = new FileOutputStream(file);

        try {
           // packager.mkjar(gp, zos);
            return saveJobForRuntime(job, zos);
        } finally {
            zos.close();
        }
    }

    public Iterable<Class> saveJobForRuntime(IJobForRuntime jobForRuntime, OutputStream outputStream) throws InternalProcessingError {
        try {
            ISerializerWithExtensibleClassListing serializer = SerializerFactory.GetInstance().borrowObject();
            try {
                serializer.toXMLAndListPlugableClasses(jobForRuntime, outputStream);
                return serializer.getExtensibleClasses();
            } finally {
                SerializerFactory.GetInstance().returnObject(serializer);
            }
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        } 

       // xstream.toXML(jobForRuntime, outputStream);

      //  return Collections.EMPTY_LIST;
    }

    
    @Override
    public void saveExecutionMessage(IExecutionMessage job, File file) throws IOException  {
        OutputStream os = new FileOutputStream(file);
        try {
            xstream.toXML(job, os);
        } finally {
            os.close();
        }
    }

    @Override
    public IExecutionMessage loadExecutionMessage(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            return (IExecutionMessage) xstream.fromXML(is);
        } finally {
            is.close();
        }
    }
}
