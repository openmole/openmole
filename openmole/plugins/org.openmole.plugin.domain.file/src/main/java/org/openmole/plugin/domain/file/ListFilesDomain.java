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

package org.openmole.plugin.domain.file;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.domain.FiniteDomain;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author reuillon
 */
public class ListFilesDomain extends FiniteDomain<File>{

    final File dir;
    final FileFilter filter;

    public ListFilesDomain(File dir) {
        this(dir, (FileFilter) null);
    }

    public ListFilesDomain(File dir, final String pattern) {
        this(dir, new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().matches(pattern);
            }
        });
    }

    public ListFilesDomain(File dir, FileFilter filter) {
        this.dir = dir;
        this.filter = filter;
    }

    @Override
    public List<File> computeValues(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        if(filter == null) return Arrays.asList(dir.listFiles());
        else return Arrays.asList(dir.listFiles(filter));
    }


}
