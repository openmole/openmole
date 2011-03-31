/*
 *  Copyright (C) 2010 Jaroslav Tulach <jtulach@netbeans.org>
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.apidesign.netbinox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.bundlefile.DirBundleFile;
import org.eclipse.osgi.baseadaptor.bundlefile.MRUBundleFileList;
import org.eclipse.osgi.baseadaptor.bundlefile.ZipBundleFile;
import org.netbeans.core.netigso.spi.BundleContent;
import org.netbeans.core.netigso.spi.NetigsoArchive;

/** This is fake bundle. It is created by the Netbinox infrastructure to 
 * use the {@link NetigsoArchive} to get cached data and speed up the start.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class JarBundleFile extends BundleFile implements BundleContent {
    private BundleFile delegate;

    private static Map<Long,File> usedIds;

    private final MRUBundleFileList mru;
    private final BaseData data;
    private final NetigsoArchive archive;
    
    JarBundleFile(
        File base, BaseData data, NetigsoArchive archive,
        MRUBundleFileList mru, boolean isBase
    ) {
        super(base);

        long id;
        if (isBase) {
            id = data.getBundleID();
        } else {
            id = 100000 + base.getPath().hashCode();
        }

        boolean assertOn = false;
        assert assertOn = true;
        if (assertOn) {
            if (usedIds == null) {
                usedIds =  new HashMap<Long, File>();
            }
            File prev = usedIds.put(id, base);
            if (prev != null && !prev.equals(base)) {
                NetbinoxFactory.LOG.log(
                    Level.WARNING,
                    "same id: {0} for {1} and {2}", // NOI18N
                    new Object[]{id, base, prev}
                );
            }
        }

        this.archive = archive.forBundle(id, this);
        this.data = data;
        this.mru = mru;
    }


    private synchronized BundleFile delegate(String who, String what) {
        if (delegate == null) {
            NetbinoxFactory.LOG.log(Level.FINE, "opening {0} because of {1} needing {2}", new Object[]{data.getLocation(), who, what});
            try {
                delegate = new ZipBundleFile(getBaseFile(), data, mru) {
                    @Override
                    protected boolean checkedOpen() {
                        try {
                            return getZipFile() != null;
                        } catch (IOException ex) {
                            final File bf = new File(getBaseFile().getPath());
                            if (bf.isDirectory()) {
                                try {
                                    delegate = new DirBundleFile(bf);
                                    return false;
                                } catch (IOException dirEx) {
                                    NetbinoxFactory.LOG.log(Level.WARNING, 
                                        "Cannot create DirBundleFile for " + bf,
                                        dirEx
                                    );
                                }
                            }
                            NetbinoxFactory.LOG.log(Level.WARNING, "Cannot open file {0}", bf);
                            if (!bf.isFile() || !bf.canRead()) {
                                delegate = EmptyBundleFile.EMPTY;
                                return false;
                            }
                        }
                        // no optimizations
                        return super.checkedOpen();
                    }
                };
            } catch (IOException ex) {
                NetbinoxFactory.LOG.log(Level.FINE, "Error creating delegate for {0}", getBaseFile());
                delegate = EmptyBundleFile.EMPTY;
            }
        }
        return delegate;
    }

    @Override
    public File getBaseFile() {
        final File file = super.getBaseFile();
        class VFile extends File {

            public VFile() {
                super(file.getPath());
            }

            @Override
            public boolean isDirectory() {
                return false;
            }

            @Override
            public File getAbsoluteFile() {
                return this;
            }
            }
        return new VFile();
    }

    @Override
    public File getFile(String file, boolean bln) {
        byte[] exists = getCachedEntry(file);
        if (exists == null) {
            return null;
        }
        BundleFile d = delegate("getFile", file);
        return d == null ? null : d.getFile(file, bln);
    }

    public byte[] resource(String name) throws IOException {
        BundleEntry u = findEntry("resource", name);
        if (u == null) {
            return null;
        }
        InputStream is = u.getInputStream();
        if (is == null) {
            return new byte[0];
        }
        byte[] arr = null;
        try {
            arr = new byte[is.available()];
            int pos = 0;
            for (;;) {
                int toRead = arr.length - pos;
                if (toRead == 0) {
                    break;
                }
                int len = is.read(arr, pos, toRead);
                if (len == -1) {
                    break;
                }
                pos += len;
            }
            if (pos != arr.length) {
                throw new IOException("Not read enough: " + pos + " should have been: " + arr.length); // NOI18N
            }
        } finally {
            is.close();
        }
        return arr;
    }

    private BundleEntry findEntry(String why, String name) {
        BundleEntry u;
        for (;;) {
            BundleFile d = delegate(why, name);
            u = d.getEntry(name);
            if (u != null || d == delegate) {
                break;
            }
        }
        return u;
    }


    private byte[] getCachedEntry(String name) {
        try {
            return archive.fromArchive(name);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public BundleEntry getEntry(final String name) {
        final byte[] arr = getCachedEntry(name);
        if (arr == null && !name.equals("/")) {
            return null;
        }
        return new BundleEntry() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(arr);
            }

            @Override
            public long getSize() {
                return arr.length;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getTime() {
                return getBaseFile().lastModified();
            }

            @Override
            public URL getLocalURL() {
                return findEntry("getLocalURL", name).getLocalURL(); // NOI18N
            }

            @Override
            public URL getFileURL() {
                return findEntry("getFileURL", name).getFileURL(); // NOI18N
            }
        };
    }

    @Override
    public Enumeration getEntryPaths(String prefix) {
        BundleFile d = delegate("getEntryPaths", prefix);
        if (d == null) {
            return Collections.enumeration(Collections.emptyList());
        }
        return d.getEntryPaths(prefix);
    }

    @Override
    public synchronized void close() throws IOException {
        if (delegate != null) {
            delegate.close();
        }
    }

    @Override
    public void open() throws IOException {
        if (delegate != null) {
            delegate.open();
        }
    }

    @Override
    public boolean containsDir(String path) {
        return path.endsWith("/") && getEntry(path) != null;
    }
}
