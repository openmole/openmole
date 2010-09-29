/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {


    private static BundleContext context;
    private static ExecutorService cleanFiles;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {

    }

    public static BundleContext getContext() {
        return context;
    }

    public static ExecutorService getCleanFiles() {
        if (cleanFiles != null) {
            return cleanFiles;
        }

        synchronized (Activator.class) {
            if (cleanFiles == null) {
                cleanFiles = Executors.newSingleThreadExecutor(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread ret = new Thread(r);
                        ret.setDaemon(true);
                        return ret;
                    }
                });

            }
        }
        return cleanFiles;
    }
}
