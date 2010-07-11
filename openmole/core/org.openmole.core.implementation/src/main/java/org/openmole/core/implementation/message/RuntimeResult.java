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
package org.openmole.core.implementation.message;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.openmole.core.model.file.IURIFile;

import org.openmole.core.model.message.IRuntimeResult;
import org.openmole.commons.tools.io.IHash;
import scala.Tuple2;

public class RuntimeResult extends RuntimeMessage implements IRuntimeResult {

    Tuple2<IURIFile, IHash> stdOut;
    Tuple2<IURIFile, IHash> stdErr;
    Tuple2<IURIFile, IHash> tarResult;
    
    Throwable exception;
    Map<String, Tuple2<File, Boolean>> files = new TreeMap<String, Tuple2<File, Boolean>>();

    IURIFile contextResultURI;
    
    
    @Override
    public Tuple2<IURIFile, IHash> getStdOut() {
        return stdOut;
    }

    @Override
    public void setStdOut(IURIFile stdOut, IHash hash) {
        this.stdOut = new Tuple2<IURIFile, IHash>(stdOut, hash);
    }

    @Override
    public Tuple2<IURIFile, IHash> getStdErr() {
        return stdErr;
    }

    @Override
    public void setStdErr(IURIFile stdErr, IHash hash) {
        this.stdErr = new Tuple2<IURIFile, IHash>(stdErr, hash);
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public Tuple2<IURIFile, IHash> getTarResult() {
        return tarResult;
    }

    @Override
    public void setTarResult(IURIFile tarResult, IHash hash) {
        this.tarResult = new Tuple2<IURIFile, IHash>(tarResult, hash);
    }

    @Override
    public void addFileName(String hash, File filePath, boolean isDirectory) {
        files.put(hash, new Tuple2<File, Boolean>(filePath, isDirectory));
    }

    @Override
    public Tuple2<File, Boolean> getFileInfoForEntry(String hash) {
        return files.get(hash);
    }

    @Override
    public IURIFile getContextResultURI() {
        return contextResultURI;
    }

    @Override
    public void setContextResultURI(IURIFile file) {
       this.contextResultURI = file;
    }

}
