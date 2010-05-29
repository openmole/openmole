/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.implementation.execution.batch;

import java.util.Collection;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.core.model.file.IURIFile;

/**
 *
 * @author reuillon
 */
public class Runtime implements IRuntime {

    final IURIFile runtime;
    final Collection<IURIFile> environmentPlugins;
    final IURIFile environmentDescritpion;

    public Runtime(IURIFile runtime, Collection<IURIFile> environmentPlugins, IURIFile environmentDescritpion) {
        this.runtime = runtime;
        this.environmentPlugins = environmentPlugins;
        this.environmentDescritpion = environmentDescritpion;
    }


    @Override
    public IURIFile getRuntime() {
        return runtime;
    }

    @Override
    public Collection<IURIFile> getEnvironmentPlugins() {
        return environmentPlugins;
    }

    @Override
    public IURIFile getEnvironmentDescriptionFile() {
        return environmentDescritpion;
    }

}
