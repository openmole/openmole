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
package org.openmole.core.implementation.message;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.openmole.core.model.file.IURIFile;

import org.openmole.core.model.message.IRuntimeResult;
import org.openmole.core.model.message.IFileMessage;
import scala.Tuple2;

public class RuntimeResult extends RuntimeMessage implements IRuntimeResult {

    final IFileMessage stdOut;
    final IFileMessage stdErr;
    final IFileMessage tarResult;
    
    final Throwable exception;
    final Map<String, Tuple2<File, Boolean>> filesInfo;

    final IURIFile contextResultURI;

    public RuntimeResult(IFileMessage stdOut, IFileMessage stdErr, IFileMessage tarResult, Map<String, Tuple2<File, Boolean>> filesInfo, Throwable exception, IURIFile contextResultURI) {
        this.stdOut = stdOut;
        this.stdErr = stdErr;
        this.tarResult = tarResult;
        this.exception = exception;
        this.contextResultURI = contextResultURI;
        this.filesInfo = filesInfo;
    }
    
    @Override
    public IFileMessage getStdOut() {
        return stdOut;
    }

    @Override
    public IFileMessage getStdErr() {
        return stdErr;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public IFileMessage getTarResult() {
        return tarResult;
    }
    
    @Override
    public Map<String, Tuple2<File, Boolean>> getFilesInfo() {
        return filesInfo;
    }

    @Override
    public IURIFile getContextResultURI() {
        return contextResultURI;
    }

}
