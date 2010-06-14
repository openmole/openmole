/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.core.implementation.resource;

import java.io.File;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.resource.ILocalFileCache;
import org.openmole.core.model.resource.IResource;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class PluginResource implements IResource {

    FileResource plugin;

    public PluginResource(String fileLocation) {
        this(new File(fileLocation));
    }


    public PluginResource(File file) {
        this.plugin = new FileResource(file);
    }

    @Override
    public void relocate(ILocalFileCache localFileCache) throws InternalProcessingError, UserBadDataError {
        plugin.relocate(localFileCache);
    }

    @Override
    public void deploy() throws InternalProcessingError, UserBadDataError {
        plugin.deploy();
        Activator.getPluginManager().load(plugin.getFile());
    }

    @Override
    public Iterable<File> getFiles() {
        return plugin.getFiles();
    }

}
