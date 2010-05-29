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
package org.openmole.core.implementation.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;


import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.resource.ILocalFileCache;
import org.openmole.core.model.resource.IResource;

import org.openmole.commons.tools.io.TarArchiver;

public class TarResource implements IResource {

    final private FileResource archive;
    final private boolean gzipped;
    transient private File deployDir;

    public TarResource(File archives, boolean gzipped) {
        super();
        this.archive = new FileResource(archives);
        this.gzipped = gzipped;
    }

    @Override
    public synchronized void deploy(ILocalFileCache filecache) throws InternalProcessingError, UserBadDataError {

        if (deployDir == null) {
            archive.deploy(filecache);
            File archiveFile = archive.getDeployedFile();

            try {
                deployDir = Activator.getWorkspace().newTmpDir("tar");
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }

            try {
                File archFile = filecache.getLocalFileCache(archiveFile);
                TarArchiver archiver = new TarArchiver();
                InputStream is = new FileInputStream(archFile);
                if (gzipped) {
                    is = new GZIPInputStream(is);
                }

                try {
                    archiver.extractDirArchiveWithRelativePath(deployDir, is);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new InternalProcessingError(e);
            }

        }

    }

    public File getDeployDir() {
        return deployDir;
    }

    @Override
    public Collection<File> getFiles() {
        return archive.getFiles();
    }
}
