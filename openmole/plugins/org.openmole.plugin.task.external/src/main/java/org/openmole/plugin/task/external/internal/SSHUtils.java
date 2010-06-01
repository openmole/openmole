/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
package org.openmole.plugin.task.external.internal;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3DirectoryEntry;
import ch.ethz.ssh2.SFTPv3FileAttributes;
import ch.ethz.ssh2.SFTPv3FileHandle;
import ch.ethz.ssh2.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.openmole.commons.tools.pattern.BufferFactory;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class SSHUtils {

    public static void copyTo(SFTPv3Client client, File local, String remote) throws IOException {

        if (local.isFile()) {
            FileInputStream fis = new FileInputStream(local);
            try {
                SFTPv3FileHandle fileHandle = client.createFileTruncate(remote);
                try {
                    copyTo(client, fis, fileHandle);
                } finally {
                    client.closeFile(fileHandle);
                }
            } finally {
                fis.close();
            }
        } else if (local.isDirectory()) {
            String nextLevel = remote + '/' + local.getName();
            client.mkdir(nextLevel, 0x777);
            for (File f : local.listFiles()) {
                copyTo(client, f, nextLevel);
            }
        }
    }

    private static void copyTo(final SFTPv3Client client, final FileInputStream local, final SFTPv3FileHandle remote) throws IOException {
        final byte[] buffer;
        try {
            buffer = BufferFactory.GetInstance().borrowObject();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        try {
            long offset = 0;

            while (true) {
                int amountRead = local.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                client.write(remote, offset, buffer, 0, amountRead);
                offset += amountRead;
            }

        } finally {
            try {
                BufferFactory.GetInstance().returnObject(buffer);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }

    public static void copyFrom(SFTPv3Client client, String remote, File local) throws IOException {

        SFTPv3FileAttributes fileAttribute = client.stat(remote);

        if (fileAttribute.isRegularFile()) {
            FileOutputStream fos = new FileOutputStream(local);
            try {
                SFTPv3FileHandle fileHandle = client.openFileRO(remote);
                try {
                    copyFrom(client, fileHandle, fos);
                } finally {
                    client.closeFile(fileHandle);
                }
            } finally {
                fos.close();
            }
        } else if (fileAttribute.isDirectory()) {
            String name = remote.substring(remote.lastIndexOf("/"));
            File nextLevel = new File(local, name);
            nextLevel.mkdir();
            for (Object entry : client.ls(remote)) {
                String entryStr = ((SFTPv3DirectoryEntry) entry).filename;
                if (!entryStr.equals("..") && !entryStr.equals(".")) {
                    copyFrom(client, name + "/" + entryStr, nextLevel);
                }
            }
        }
    }

    private static void copyFrom(SFTPv3Client client, SFTPv3FileHandle remote, OutputStream local) throws IOException {
        final byte[] buffer;
        try {
            buffer = BufferFactory.GetInstance().borrowObject();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        try {
            long offset = 0;

            while (true) {
                int amountRead = client.read(remote, offset, buffer, 0, buffer.length);
                if (amountRead == -1) {
                    break;
                }
                local.write(buffer, 0, amountRead);
                offset += amountRead;
            }

        } finally {
            try {
                BufferFactory.GetInstance().returnObject(buffer);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }

    public static void delete(SFTPv3Client client, String path) throws IOException {
        if (!client.stat(path).isDirectory()) {
            client.rm(path);
        } else {
            for (Object entry : client.ls(path)) {
                String entryStr = ((SFTPv3DirectoryEntry) entry).filename;
                if (!entryStr.equals("..") && !entryStr.equals(".")) {
                    delete(client, path + '/' + entryStr);
                }
            }
            client.rmdir(path);
        }
    }

    public static void waitForCommandToEnd(Session session, int commandWait) throws IOException {
        byte[] buffer = new byte[8192];
        InputStream stdout = session.getStdout();
        InputStream stderr = session.getStderr();

        while (true) {
            if ((stdout.available() == 0) && (stderr.available() == 0)) {

                int conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA
                        | ChannelCondition.EOF, commandWait);

                if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                    throw new IOException("Timeout while waiting for data from peer.");
                }

                if ((conditions & ChannelCondition.EOF) != 0) {
                    if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                        break;
                    }
                }
            }

            while (stdout.available() > 0) {
                int len = stdout.read(buffer);
                if (len > 0) // this check is somewhat paranoid
                {
                    System.out.write(buffer, 0, len);
                }
            }

            while (stderr.available() > 0) {
                int len = stderr.read(buffer);
                if (len > 0) // this check is somewhat paranoid
                {
                    System.err.write(buffer, 0, len);
                }
            }
        }
    }
}
