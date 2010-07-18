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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.openmole.core.model.file.IURIFile;

/**
 *
 * @author reuillon
 */
public class BZ2URIFile extends URIFile {

    public BZ2URIFile(IURIFile file) {
        super(file);
    }
    
    @Override
    public InputStream openInputStream() throws IOException, InterruptedException {
        return new BZip2CompressorInputStream(super.openInputStream());
    }

    @Override
    public OutputStream openOutputStream() throws IOException, InterruptedException {
        return new BZip2CompressorOutputStream(super.openOutputStream());
    }
}
