/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.core.implementation.tools;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.resource.ILocalFileCache;


/*TODO suport arrays and set*/
public class FileMigrator {

    public static void initFilesInVariable(IVariable variable, ILocalFileCache fileCache) {
        if (File.class.isAssignableFrom(variable.getPrototype().getType())) {
            initFile((IVariable<File>) variable, fileCache);
        } else {
            if (List.class.isAssignableFrom(variable.getPrototype().getType())) {
                initFilesInList(((IVariable<List>) variable).getValue(), fileCache);
            }
        }
    }

    public static void initFilesInVariables(Iterable<IVariable> context, ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {
        for (IVariable v : context) {
            initFilesInVariable(v, fileCache);
        }
    }

    private static void initFilesInList(List list, ILocalFileCache fileCache) {
        int i = 0;
        for (Object o : list) {
            //Object o = list.get(i);
            if (File.class.isAssignableFrom(o.getClass())) {
                File src = (File) o;
                File local = fileCache.getLocalFileCache(src);

                list.set(i, local);

            } else if (List.class.isAssignableFrom(o.getClass())) {
                initFilesInList((List) o, fileCache);
            }
            i++;
        }

    }

    private static void initFile(IVariable<File> v, ILocalFileCache fileCache) {
        File src = v.getValue();
        File local = fileCache.getLocalFileCache(src);
        v.setValue(local);
    }

    public static Set<File> extractFilesFromVariable(IVariable variable) {
        Set<File> fileMap = new TreeSet<File>();
        if (File.class.isAssignableFrom(variable.getPrototype().getType())) {
            fileMap.add(((IVariable<File>) variable).getValue());
        } else {
            if (List.class.isAssignableFrom(variable.getPrototype().getType())) {
                extractFilesFromList(((IVariable<List>) variable).getValue(), fileMap);
            }
        }
        return fileMap;
    }

    public static Set<File> extractFilesFromVariables(Iterable<IVariable> context) {
        Set<File> fileMap = new TreeSet<File>();

        for (IVariable<?> v : context) {
            fileMap.addAll(extractFilesFromVariable(v));
        }
        return fileMap;
    }

    private static void extractFilesFromList(List list, Set<File> fileMap) {
        for (Object o : list) {

            if (File.class.isAssignableFrom(o.getClass())) {
                fileMap.add((File) o);
            } else {
                if (List.class.isAssignableFrom(o.getClass())) {
                    extractFilesFromList((List) o, fileMap);
                }
            }

        }
    }
}
