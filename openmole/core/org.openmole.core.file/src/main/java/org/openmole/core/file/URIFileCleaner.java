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
package org.openmole.core.file;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.core.model.file.IURIFile;

public class URIFileCleaner implements Runnable {

    final static Logger LOGGER = Logger.getLogger(URIFileCleaner.class.getName());

    IURIFile toClean;
    boolean timeOut = true;
    boolean recursive = false;

    public URIFileCleaner(IURIFile toClean, boolean recursive) {
        super();
        this.toClean = toClean;
        this.recursive = recursive;
    }

    public URIFileCleaner(IURIFile toClean, boolean recursive, boolean timeOut) {
        super();
        this.toClean = toClean;
        this.timeOut = timeOut;
        this.recursive = recursive;
    }

    @Override
    public void run() {
        try {
            if (toClean != null) {
                toClean.remove(timeOut, recursive);
                LOGGER.log(Level.FINE, "Cleaned {0}", toClean.toString());
            }
        } catch (IOException e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.FINE, "Cannot delete file " + toClean.getLocation(), e);
        } catch (InterruptedException e) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.FINE, "Cannot delete file " + toClean.getLocation(), e);
        }

    }
}
