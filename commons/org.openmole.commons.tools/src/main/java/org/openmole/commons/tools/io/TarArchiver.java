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

package org.openmole.commons.tools.io;

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
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class TarArchiver implements IArchiver {


    private interface IAdditionnalCommand {
        void apply(TarArchiveEntry e);
    }

    @Override
    public void createDirArchiveWithRelativePathNoVariableContent(final File baseDir, final OutputStream archive) throws IOException {
        createDirArchiveWithRelativePathWithAdditionnalCommand(baseDir, archive, new IAdditionnalCommand() {
            @Override
            public void apply(TarArchiveEntry e) {
                e.setModTime(0);
            }
        });
    }

    @Override
    public void createDirArchiveWithRelativePath(final File baseDir, final OutputStream archive) throws IOException {
        createDirArchiveWithRelativePathWithAdditionnalCommand(baseDir, archive, new IAdditionnalCommand() {
            @Override
            public void apply(TarArchiveEntry e) {}
        });

    }

    private void createDirArchiveWithRelativePathWithAdditionnalCommand(final File baseDir, final OutputStream archive, final IAdditionnalCommand additionnalCommand) throws IOException {
        if (!baseDir.isDirectory()) {
            throw new IOException(baseDir.getAbsolutePath() + " is not a directory.");
        }

        TarArchiveOutputStream tos = new TarArchiveOutputStream(archive);

        try {
            Stack<Tuple2<File, String>> toArchive = new Stack<Tuple2<File, String>>();
            toArchive.push(new Tuple2<File, String>(baseDir, ""));

            while (!toArchive.isEmpty()) {
                Tuple2<File, String> cur = toArchive.pop();

                if (cur._1().isDirectory()) {
                    for (String name : cur._1().list()) {
                        toArchive.push(new Tuple2<File, String>(new File(cur._1(), name), cur._2() + '/' + name));
                    }
                } else {
                    TarArchiveEntry e = new TarArchiveEntry(cur._2());
                    e.setSize(cur._1().length());
                    additionnalCommand.apply(e);
                    tos.putArchiveEntry(e);
                    try {
                        FileUtil.copy(new FileInputStream(cur._1()), tos);
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
                FileUtil.copy(tis, new FileOutputStream(dest));
            }
        } finally {
            tis.close();
        }

    }
}
