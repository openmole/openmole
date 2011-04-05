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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.Manifest;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/** This is fake bundle, created by the Netigso infrastructure.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class NetigsoBundleFile extends BundleFile {
    private final BundleData data;
    NetigsoBundleFile(File base, BundleData data) {
        super(base);
        this.data = data;
    }

    @Override
    public File getFile(String string, boolean bln) {
        return null;
    }

    @Override
    public BundleEntry getEntry(String entry) {
        if ("META-INF/MANIFEST.MF".equals(entry)) { // NOI18N
            return new BundleEntry() {
                @Override
                public InputStream getInputStream() throws IOException {
                    for (ModuleInfo mi : Lookup.getDefault().lookupAll(ModuleInfo.class)) {
                        if (data.getLocation().endsWith(mi.getCodeNameBase())) {
                            return fakeManifest(mi);
                        }
                    }
                    throw new IOException("Cannot find " + data.getLocation());
                }

                @Override
                public long getSize() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public String getName() {
                    return "META-INF/MANIFEST.MF"; // NOI18N
                }

                @Override
                public long getTime() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public URL getLocalURL() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public URL getFileURL() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        }
        return null;
    }

    @Override
    public Enumeration getEntryPaths(String string) {
        return Collections.enumeration(Collections.emptyList());
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public boolean containsDir(String string) {
        return false;
    }

    private static InputStream fakeManifest(ModuleInfo m) throws IOException {
        String exp = (String) m.getAttribute("OpenIDE-Module-Public-Packages"); // NOI18N
        if ("-".equals(exp)) { // NOI18N
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Manifest man = new Manifest();
        man.getMainAttributes().putValue("Manifest-Version", "1.0"); // workaround for JDK bug
        man.getMainAttributes().putValue("Bundle-ManifestVersion", "2"); // NOI18N
        man.getMainAttributes().putValue("Bundle-SymbolicName", m.getCodeNameBase()); // NOI18N

        if (m.getSpecificationVersion() != null) {
            String spec = threeDotsWithMajor(m.getSpecificationVersion().toString(), m.getCodeName());
            man.getMainAttributes().putValue("Bundle-Version", spec.toString()); // NOI18N
        }
        if (exp != null) {
            man.getMainAttributes().putValue("Export-Package", exp.replaceAll("\\.\\*", "")); // NOI18N
        } else {
            man.getMainAttributes().putValue("Export-Package", m.getCodeNameBase()); // NOI18N
        }
        man.write(os);
        return new ByteArrayInputStream(os.toByteArray());
    }
    private static String threeDotsWithMajor(String version, String withMajor) {
        int indx = withMajor.indexOf('/');
        int major = 0;
        if (indx > 0) {
            major = Integer.parseInt(withMajor.substring(indx + 1));
        }
        String[] segments = (version + ".0.0.0").split("\\.");
        assert segments.length >= 3 && segments[0].length() > 0;

        return (Integer.parseInt(segments[0]) + major * 100) + "."  + segments[1] + "." + segments[2];
    }
}
