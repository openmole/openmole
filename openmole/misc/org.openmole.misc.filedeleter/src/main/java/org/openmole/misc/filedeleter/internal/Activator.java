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
package org.openmole.misc.filedeleter.internal;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.filedeleter.IFileDeleter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {

    private static BundleContext context;
    private ServiceRegistration deleterReg;
    private static FileDeleter deleter;
    
    
    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        deleter = new FileDeleter();
        deleterReg = context.registerService(IFileDeleter.class.getName(), deleter, null);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        deleterReg.unregister();
        deleter.stop();
        deleter = null;
    }

    public static BundleContext getContext() {
        return context;
    }

    static IFileDeleter getDeleter() {
        return deleter;
    }
    
    
}
