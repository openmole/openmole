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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class SSHUtils {

    public static void copyTo(ChannelSftp channel, File local, String remote) throws SftpException, IOException {

        if (local.isFile()) {
            FileInputStream fis = new FileInputStream(local);
            try {
                channel.put(fis, remote);
            } finally {
                fis.close();
            }
        } else if (local.isDirectory()) {
            String nextLevel = remote + '/' + local.getName();
            channel.mkdir(nextLevel);
            for (File f : local.listFiles()) {
                copyTo(channel, f, nextLevel);
            }
        }
    }

    public static void copyFrom(ChannelSftp channel, String remote, File local) throws SftpException, IOException {
        String name = remote.substring(remote.lastIndexOf("/"));

        if (!channel.stat(remote).isDir()) {
            FileOutputStream fos = new FileOutputStream(local);
            try {
                channel.get(remote, fos);
            } finally {
                fos.close();
            }
        } else {
            File nextLevel = new File(local, name);
            nextLevel.mkdir();
            for (Object entry : channel.ls(remote)) {
                String entryStr = ((ChannelSftp.LsEntry) entry).getFilename();
                if (!entryStr.equals("..") && !entryStr.equals(".")) {
                    copyFrom(channel, name + "/" + entryStr, nextLevel);
                }
            }
        }
    }

    public static void delete(ChannelSftp client, String path) throws SftpException {
        if (!client.stat(path).isDir()) {
            client.rm(path);
        } else {
            for (Object entry : client.ls(path)) {
                String entryStr = ((ChannelSftp.LsEntry) entry).getFilename();
                if (!entryStr.equals("..") && !entryStr.equals(".")) {
                    delete(client, path + '/' + entryStr);
                }
            }
            client.rmdir(path);
        }
    }
}
