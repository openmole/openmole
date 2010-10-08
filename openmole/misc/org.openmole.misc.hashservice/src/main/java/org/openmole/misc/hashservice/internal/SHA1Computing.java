/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.misc.hashservice.internal;

import gnu.crypto.hash.IMessageDigest;
import gnu.crypto.hash.Sha160;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openmole.commons.tools.io.FileUtil;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.misc.hashservice.IHashService;
import org.openmole.misc.hashservice.SHA1Hash;
import org.openmole.commons.tools.io.ReaderRunnable;
import org.openmole.commons.tools.io.FileInputStream;

public class SHA1Computing implements IHashService {

  /*  private ObjectPool sha1Pool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {

        @Override
        public Object makeObject() throws Exception {
            return new Sha160();
        }

        @Override
        public void passivateObject(Object obj) throws Exception {
            ((Sha160) obj).reset();
            super.passivateObject(obj);
        }
    });*/

    @Override
    public SHA1Hash computeHash(File file) throws IOException {

        InputStream is = new FileInputStream(file);

        try {
            return computeHash(is);
        } finally {
            is.close();
        }

    }

    @Override
    public SHA1Hash computeHash(InputStream is) throws IOException {
        
        final byte[] buffer = new byte[FileUtil.DEFAULT_BUFF_SIZE];
        final IMessageDigest md = new Sha160();

       // IMessageDigest md;

       /* byte[] buffer;
        try {
            buffer = (byte[]) BufferFactory.GetInstance().borrowObject();
        } catch (NoSuchElementException e) {
            throw new IOException(e);
        } catch (IllegalStateException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }

        try {
            try {
                md = (IMessageDigest) sha1Pool.borrowObject();
            } catch (NoSuchElementException e) {
                throw new IOException(e);
            } catch (IllegalStateException e) {
                throw new IOException(e);
            } catch (Exception e) {
                throw new IOException(e);
            }

            try {*/
                while (true) {
                    int amount = is.read(buffer);
                    if (amount == -1) {
                        break;
                    }
                    md.update(buffer, 0, amount);
                }
                return new SHA1Hash(md.digest());

        /*    } finally {
                try {
                    sha1Pool.returnObject(md);
                } catch (Exception e) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Unable to return object to the pool: memomry leak.", e);
                }
            }

        } finally {
            try {
                BufferFactory.GetInstance().returnObject(buffer);
            } catch (Exception e) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Unable to return object to the pool: memomry leak.", e);
            }
        }*/

    }

    @Override
    public SHA1Hash computeHash(InputStream is, int maxRead, long timeout) throws IOException {
       /* if (maxRead > BufferFactory.MAX_BUFF_SIZE) {
            throw new IOException("Max buffer size is " + BufferFactory.MAX_BUFF_SIZE + " unable to evaluate timeout on " + maxRead + " bytes.");
        }*/

        final byte[] buffer = new byte[maxRead];
        final IMessageDigest md = new Sha160();

        ExecutorService thread = Activator.getExecutorService().getExecutorService(ExecutorType.OWN);

   //     try {
               //md = new Sha160();

           
           /* try {
                buffer = (byte[]) BufferFactory.GetInstance().borrowObject();
            } catch (NoSuchElementException e) {
                throw new IOException(e);
            } catch (IllegalStateException e) {
                throw new IOException(e);
            } catch (Exception e) {
                throw new IOException(e);
            }

            try {


                try {
                    md = (IMessageDigest) sha1Pool.borrowObject();
                } catch (NoSuchElementException e) {
                    throw new IOException(e);
                } catch (IllegalStateException e) {
                    throw new IOException(e);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                try */
                    int read;
                    ReaderRunnable reader = new ReaderRunnable(is, buffer, maxRead);

                    while (true) {
                        Future<Integer> f = thread.submit(reader);

                        try {
                            read = f.get(timeout, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e1) {
                            throw new IOException(e1);
                        } catch (ExecutionException e1) {
                            throw new IOException(e1);
                        } catch (TimeoutException e1) {
                            f.cancel(true);
                            throw new IOException("Timout on reading, read was longer than " + timeout, e1);
                        }

                        if (read == -1) {
                            break;
                        }


                        md.update(buffer, 0, read);

                    }
                    
                /*} finally {
                    try {
                        sha1Pool.returnObject(md);
                    } catch (Exception e) {
                        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Unable to return object to the pool: memomry leak.", e);
                    }
                }
            } finally {
                try {
                    BufferFactory.GetInstance().returnObject(buffer);
                } catch (Exception e) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Unable to return object to the pool: memomry leak.", e);
                }
            }*/
     /*   } finally {
            thread.shutdownNow();
        }*/

        return new SHA1Hash(md.digest());
    }
}
