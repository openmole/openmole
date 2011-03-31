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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class EmptyBundleFile extends BundleFile {
    public static final BundleFile EMPTY = new EmptyBundleFile();

    private EmptyBundleFile() {
    }

    @Override
    public File getFile(String string, boolean bln) {
        return null;
    }

    @Override
    public BundleEntry getEntry(String string) {
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
}
