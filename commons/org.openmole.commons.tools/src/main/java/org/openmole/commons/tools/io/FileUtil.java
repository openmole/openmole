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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openmole.commons.tools.pattern.BufferFactory;
import scala.Tuple2;

public class FileUtil {

    final static ExecutorService pool = Executors.newCachedThreadPool();

    public static long getLastModification(File file) {

        long lastModification = file.lastModified();

        if (file.isDirectory()) {
            Queue<File> toProceed = new LinkedList<File>();
            toProceed.offer(file);

            while (!toProceed.isEmpty()) {
                File f = toProceed.poll();

                if (f.lastModified() > lastModification) {
                    lastModification = f.lastModified();
                }
                if (f.isDirectory()) {
                    for (File child : f.listFiles()) {
                        toProceed.offer(child);
                    }
                }
            }
        }

        return lastModification;
    }

    public static void applyRecursive(File file, IFileOperation operation) {
        applyRecursive(file, operation, Collections.EMPTY_SET);
    }

    public static void applyRecursive(File file, IFileOperation operation, Set<File> stopPath) {
        Queue<File> toProceed = new LinkedList<File>();
        toProceed.offer(file);

        while (!toProceed.isEmpty()) {
            File f = toProceed.poll();
            if (!stopPath.contains(f)) {
                operation.execute(f);
                if (f.isDirectory()) {
                    for (File child : f.listFiles()) {
                        toProceed.offer(child);
                    }
                }
            }
        }
    }

    public static boolean dirContainsNoFileRecursive(File dir) {
        Queue<File> toProceed = new LinkedList<File>();
        toProceed.add(dir);

        while (!toProceed.isEmpty()) {
            File f = toProceed.poll();
            for (File sub : f.listFiles()) {
                if (sub.isFile()) {
                    return false;
                } else if(sub.isDirectory()) {
                    toProceed.offer(sub);
                }
            }
        }
        return true;
    }

    static public boolean recursiveDelete(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    recursiveDelete(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return dir.delete();
    }

    public static void copy(File fromF, File toF) throws IOException {
        Queue<Tuple2<File, File>> toCopy = new LinkedList<Tuple2<File, File>>();
        toCopy.offer(new Tuple2<File, File>(fromF, toF));

        while (!toCopy.isEmpty()) {
            Tuple2<File, File> cur = toCopy.poll();
            File curFrom = cur._1();
            File curTo = cur._2();
            if (curFrom.isDirectory()) {

                curTo.mkdir();

                for (File child : curFrom.listFiles()) {
                    File to = new File(curTo, child.getName());
                    toCopy.offer(new Tuple2<File, File>(child, to));
                }
            } else {
                copyFile(curFrom, curTo);
            }
        }

    }

    static void copyFile(File fromF, File toF) throws IOException {
        FileInputStream from = new FileInputStream(fromF);

        try {
            FileOutputStream to = new FileOutputStream(toF);
            try {
                copy(from.getChannel(), to.getChannel());
            } finally {
                to.close();
            }
        } finally {
            from.close();
        }
    }

    public static void copy(FileChannel source, FileChannel destination) throws IOException {
        destination.transferFrom(source, 0, source.size());
    }

    public static void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buffer;
        try {
            buffer = BufferFactory.GetInstance().borrowObject();
        } catch (NoSuchElementException e) {
            throw new IOException(e);
        } catch (IllegalStateException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }

        try {
            while (true) {
                int amountRead = from.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                to.write(buffer, 0, amountRead);
            }
        } finally {
            try {
                BufferFactory.GetInstance().returnObject(buffer);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    public static void copy(final InputStream from, final OutputStream to, int maxRead, long timeout) throws IOException, InterruptedException {

        if (maxRead > BufferFactory.MAX_BUFF_SIZE) {
            throw new IOException("Max buffer size is " + BufferFactory.MAX_BUFF_SIZE + " unable to evaluate timeout on " + maxRead + " bytes.");
        }


        byte[] buffer;
        try {
            buffer = BufferFactory.GetInstance().borrowObject();
        } catch (NoSuchElementException e) {
            throw new IOException(e);
        } catch (IllegalStateException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }

        try {
            ReaderRunnable reader = new ReaderRunnable(from, buffer, maxRead);
            WritterRunnable writer = new WritterRunnable(to, buffer);

            while (true) {
                Future<?> f = pool.submit(reader);

                try {
                    f.get(timeout, TimeUnit.MILLISECONDS);
                    if (reader.getException() != null) {
                        throw reader.getException();
                    }
                } catch (ExecutionException e1) {
                    throw new IOException(e1);
                } catch (TimeoutException e1) {
                    f.cancel(true);
                    throw new IOException("Timout on reading " + maxRead + " bytes, read was longer than " + timeout + "ms.", e1);
                }


                if (reader.getAmountRead() == -1) {
                    break;
                }

                writer.setAmountRead(reader.getAmountRead());
                f = pool.submit(writer);

                try {
                    f.get(timeout, TimeUnit.MILLISECONDS);
                    if (writer.getException() != null) {
                        throw writer.getException();
                    }
                } catch (ExecutionException e1) {
                    throw new IOException(e1);
                } catch (TimeoutException e1) {
                    f.cancel(true);
                    throw new IOException("Timeout on writting " + reader.getAmountRead() + " bytes, write was longer than " + timeout + " ms.", e1);
                }
            }
        } finally {
            try {
                BufferFactory.GetInstance().returnObject(buffer);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
    
    public static void move(File from, File to) throws IOException {
        if(!from.renameTo(to)) {
            copy(from, to);
            from.delete();
        }
    }
    
}
