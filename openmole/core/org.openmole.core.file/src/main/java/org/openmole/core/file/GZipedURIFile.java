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

package org.openmole.core.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.file.internal.Activator;
import org.openmole.core.model.file.IURIFile;
import org.openmole.commons.tools.filecache.FileCacheDeleteOnFinalize;
import org.openmole.commons.tools.filecache.IFileCache;

public class GZipedURIFile extends URIFile {
	
	public GZipedURIFile(IURIFile file) {
		super(file);
	}

	public InputStream openInputStream() throws IOException, InterruptedException {
		return new GZIPInputStream(super.openInputStream());
	}

	public OutputStream openOutputStream() throws IOException, InterruptedException {
		return new GZIPOutputStream(super.openOutputStream());
	}

	
	//@Cachable
	@Override
	public IFileCache cache() throws IOException, InterruptedException {
		try {
			File cache = Activator.getWorkspace().newTmpFile("file", "cache");
			URIFile.copy(this, new URIFile(cache));
			return new FileCacheDeleteOnFinalize(cache);
		} catch (InternalProcessingError e) {
			throw new IOException(e);
		}
	}


	
}
