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
import java.util.ArrayList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.domain.FiniteDomain;
import org.openmole.core.model.job.IContext;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class ListFilesAndFileNamesDomain extends FiniteDomain<Tuple2<File, String>> {

    final File dir;
    final FileFilter filter;

    
    public ListFilesAndFileNamesDomain(String dir) {
        this(new File(dir));
    }

    public ListFilesAndFileNamesDomain(String dir, final String pattern) {
        this(new File(dir), pattern);
    }

    public ListFilesAndFileNamesDomain(String dir, FileFilter pattern) {
        this(new File(dir), pattern);
    }
    
    public ListFilesAndFileNamesDomain(File dir) {
        this(dir, (FileFilter) null);
    }

    public ListFilesAndFileNamesDomain(File dir, final String pattern) {
        this(dir, new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().matches(pattern);
            }
        });
    }

    public ListFilesAndFileNamesDomain(File dir, FileFilter pattern) {
        this.dir = dir;
        this.filter = pattern;
    }

    @Override
    public List<Tuple2<File, String>> computeValues(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        File files[];

        if (filter == null) {
            files = dir.listFiles();
        } else {
            files = dir.listFiles(filter);
        }

        List<Tuple2<File, String>> ret = new ArrayList<Tuple2<File, String>>(files.length);
        for (File f : files) {
            ret.add(new Tuple2<File, String>(f, f.getName()));
        }

        return ret;
    }
}
