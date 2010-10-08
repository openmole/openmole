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
package org.openmole.misc.filecache.internal;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {


    private static BundleContext context;
    final private static BlockingQueue<File> cleanFiles = new LinkedBlockingQueue<File>();
    private Thread thread;
    
    
    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                boolean finished = false;
                
                while(!finished) {
                    try {
                        cleanFiles.take().delete();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Activator.class.getName()).log(Level.INFO, "File deleter interupted", ex);
                        finished = true;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        thread.interrupt();
        thread = null;
    }

    public static BundleContext getContext() {
        return context;
    }

    public static void clean(File file) {
        cleanFiles.add(file);
    }
}
