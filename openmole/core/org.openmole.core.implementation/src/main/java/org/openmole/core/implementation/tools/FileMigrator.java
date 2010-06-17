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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.resource.ILocalFileCache;


/*TODO suport arrays and set*/
public class FileMigrator {

    public static void initFilesInVariable(IVariable variable, ILocalFileCache fileCache) {
        Object variableContent = variable.getValue();
        if(variableContent == null) return;

        if (File.class.isAssignableFrom(variableContent.getClass())) {
            initFile((IVariable<File>) variable, fileCache);
        } else {
            if (List.class.isAssignableFrom(variableContent.getClass())) {
                initFilesInList((List) variableContent, fileCache);
            }
        }
    }

    public static void initFilesInVariables(Iterable<IVariable> context, ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {
        for (IVariable v : context) {
            initFilesInVariable(v, fileCache);
        }
    }

    private static void initFilesInList(List list, ILocalFileCache fileCache) {
        ListIterator iterator = list.listIterator();

        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (File.class.isAssignableFrom(o.getClass())) {
                File src = (File) o;
                File local = fileCache.getLocalFileCache(src);

                iterator.set(local);
            } else if (List.class.isAssignableFrom(o.getClass())) {
                initFilesInList((List) o, fileCache);
            }
        }

    }

    private static void initFile(IVariable<File> v, ILocalFileCache fileCache) {
        File src = v.getValue();
        File local = fileCache.getLocalFileCache(src);
        v.setValue(local);
    }

    public static Set<File> extractFilesFromVariable(IVariable variable) {
        Object variableContent = variable.getValue();
        if(variableContent == null) return Collections.EMPTY_SET;

        Set<File> fileMap = new TreeSet<File>();

        if (File.class.isAssignableFrom(variableContent.getClass())) {
            fileMap.add((File) variableContent);
        } else {
            if (List.class.isAssignableFrom(variableContent.getClass())) {
                extractFilesFromList((List) variableContent, fileMap);
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
