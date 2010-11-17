/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.misc.filedeleter.internal;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.filedeleter.IFileDeleter;

/**
 *
 * @author reuillon
 */
public class FileDeleter implements IFileDeleter {

    final private static BlockingQueue<File> cleanFiles = new LinkedBlockingQueue<File>();
    final private Thread thread;
    final private Map<File, DeleteOnFinalize> deleters = new WeakHashMap<File, DeleteOnFinalize>();

    public FileDeleter() {
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                boolean finished = false;

                while (!finished) {
                    try {
                        cleanFiles.take().delete();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FileDeleter.class.getName()).log(Level.INFO, "File deleter interupted", ex);
                        finished = true;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    void stop() {
        thread.interrupt();
    }

    @Override
    public void assynchonousRemove(File file) {
        cleanFiles.add(file);
    }

    @Override
    public void deleteWhenGarbageCollected(File file) {
        deleters.put(file, new DeleteOnFinalize(file.getAbsolutePath()));
    }
    
}
