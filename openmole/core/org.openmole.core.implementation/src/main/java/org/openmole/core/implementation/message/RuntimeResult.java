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

import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.core.workflow.model.message.IRuntimeResult;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.job.IMoleJobId;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.io.IHash;


public class RuntimeResult extends RuntimeMessage implements IRuntimeResult {

	Duo<IURIFile, IHash> stdOut;
	Duo<IURIFile, IHash> stdErr;
	Duo<IURIFile, IHash> tarResult;

	Throwable exception;

	Map<IMoleJobId,IContext> results = new TreeMap<IMoleJobId,IContext>();
	Map<String, Duo<File, Boolean>> files = new TreeMap<String, Duo<File, Boolean>>();
	

	@Override
	public Duo<IURIFile, IHash> getStdOut() {
		return stdOut;
	}

	@Override
	public void setStdOut(IURIFile stdOut, IHash hash) {
		this.stdOut = new Duo<IURIFile, IHash>(stdOut, hash);
	}

	@Override
	public Duo<IURIFile, IHash> getStdErr() {
		return stdErr;
	}

	@Override
	public void setStdErr(IURIFile stdErr, IHash hash) {
		this.stdErr = new Duo<IURIFile, IHash>(stdErr, hash);
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
	public IContext getContextForJob(IMoleJobId jobId) {
		return results.get(jobId);
	}

	@Override
	public void putResult(IMoleJobId jobId, IContext context) {
		results.put(jobId, context);
	}

	@Override
	public boolean containsResultForJob(IMoleJobId jobId) {
		return results.containsKey(jobId);
	}

	@Override
	public Duo<IURIFile, IHash> getTarResult() {
		return tarResult;
	}

	@Override
	public void setTarResult(IURIFile tarResult, IHash hash) {
		this.tarResult = new Duo<IURIFile, IHash>(tarResult, hash);
	}

	@Override
	public void addFileName(String hash, File filePath, boolean isDirectory) {
		files.put(hash, new Duo<File, Boolean>(filePath, isDirectory));
	}

	@Override
	public Duo<File, Boolean> getFileInfoForEntry(String hash) {
		return files.get(hash);
	}

}
