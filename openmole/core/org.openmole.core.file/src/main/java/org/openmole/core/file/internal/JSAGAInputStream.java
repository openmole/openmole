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

package org.openmole.core.file.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.file.FileInputStream;
import org.ogf.saga.task.Task;
import org.ogf.saga.task.TaskMode;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.file.URIFile;

/**
 *
 * @author reuillon
 */
public class JSAGAInputStream extends InputStream {

    FileInputStream stream;

    public JSAGAInputStream(FileInputStream stream) {
        this.stream = stream;
    }

    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    public synchronized void reset() throws IOException {
        stream.reset();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        return stream.read(b);
    }

    public int read() throws IOException {
        return stream.read();
    }

    public boolean markSupported() {
        return stream.markSupported();
    }

    public synchronized void mark(int readlimit) {
        stream.mark(readlimit);
    }

    public void close() throws IOException {
        Task<?, ?> task = null;
        try {
            task = stream.close(TaskMode.ASYNC);
        } catch (NotImplementedException e) {
            throw new IOException(e);
        }
        try {
            task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(URIFile.Timeout), TimeUnit.MILLISECONDS);
        } catch (InternalProcessingError e) {
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    public int available() throws IOException {
        return stream.available();
    }
}
