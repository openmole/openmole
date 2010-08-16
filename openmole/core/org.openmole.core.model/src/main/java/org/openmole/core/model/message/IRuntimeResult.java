/*
 *  Copyright (C) 2010 Romain Reuillon
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
package org.openmole.core.model.message;

import java.io.File;

import org.openmole.core.model.file.IURIFile;
import scala.Tuple2;

public interface IRuntimeResult extends IRuntimeMessage {

    void setException(Throwable exception);

    Throwable getException();

    void setStdErr(IFileMessage stdErr);

    IFileMessage getStdErr();

    void setStdOut(IFileMessage stdOut);

    IFileMessage getStdOut();
    
    IFileMessage getTarResult();

    void setTarResult(IFileMessage tarResult);

    void addFileName(String hash, File filePath, boolean isDirectory);

    Tuple2<File, Boolean> getFileInfoForEntry(String hash);
    
    IURIFile getContextResultURI();
    
    void setContextResultURI(IURIFile file);
}
