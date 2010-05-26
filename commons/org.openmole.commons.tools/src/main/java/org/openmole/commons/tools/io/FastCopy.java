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
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openmole.commons.tools.pattern.BufferFactory;
import org.openmole.commons.tools.structure.Duo;

public class FastCopy {

    final static ExecutorService pool = Executors.newCachedThreadPool();


    public static void applyRecursive(File file, IFileOperation operation) {
        Stack<File> toProceed = new Stack<File>();
        toProceed.push(file);

        while(!toProceed.isEmpty()) {
            File f = toProceed.pop();
            operation.execute(f);
            if(f.isDirectory()) {
                for(File child : f.listFiles()) {
                    toProceed.add(child);
                }
            }
        }

    }



    public static void copy(File fromF, File toF) throws IOException {

        Stack<Duo<File,File>> toCopy = new Stack<Duo<File,File>>();
        toCopy.add(new Duo<File, File>(fromF, toF));

        while(!toCopy.isEmpty()) {
            Duo<File,File> cur = toCopy.pop();
            File curFrom = cur.getLeft();
            File curTo = cur.getRight();
            if (curFrom.isDirectory()) {

                curTo.mkdir();

                for(File child : curFrom.listFiles()) {
                    File to = new File(curTo, child.getName());
                    toCopy.push(new Duo<File, File>(child, to));
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
}
