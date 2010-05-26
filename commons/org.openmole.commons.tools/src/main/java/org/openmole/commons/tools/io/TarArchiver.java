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

package org.openmole.commons.tools.io;

import org.openmole.commons.tools.structure.Duo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 *
 * @author reuillon
 */
public class TarArchiver implements IArchiver {

    @Override
    public void createDirArchiveWithRelativePath(final File baseDir, final OutputStream archive) throws IOException {

        if (!baseDir.isDirectory()) {
            throw new IOException(baseDir.getAbsolutePath() + " is not a directory.");
        }

        TarArchiveOutputStream tos = new TarArchiveOutputStream(archive);

        try {
            Stack<Duo<File, String>> toArchive = new Stack<Duo<File, String>>();
            toArchive.push(new Duo<File, String>(baseDir, ""));

            while (!toArchive.isEmpty()) {
                Duo<File, String> cur = toArchive.pop();

                if (cur.getLeft().isDirectory()) {
                    for (String name : cur.getLeft().list()) {
                        toArchive.push(new Duo<File, String>(new File(cur.getLeft(), name), cur.getRight() + '/' + name));
                    }
                } else {
                    TarArchiveEntry e = new TarArchiveEntry(cur.getRight());
                    e.setSize(cur.getLeft().length());
                    tos.putArchiveEntry(e);
                    try {
                        FastCopy.copy(new FileInputStream(cur.getLeft()), tos);
                    } finally {
                        tos.closeArchiveEntry();
                    }
                }
            }
        } finally {
            tos.close();
        }
    }

    @Override
    public void extractDirArchiveWithRelativePath(final File baseDir, final InputStream archive) throws IOException {
        if (!baseDir.isDirectory()) {
            throw new IOException(baseDir.getAbsolutePath() + " is not a directory.");
        }
        TarArchiveInputStream tis = new TarArchiveInputStream(archive);

        try {
            TarArchiveEntry e;
            while ((e = tis.getNextTarEntry()) != null) {
                File dest = new File(baseDir, e.getName());
                dest.getParentFile().mkdirs();
                FastCopy.copy(tis, new FileOutputStream(dest));
            }
        } finally {
            tis.close();
        }

    }
}
