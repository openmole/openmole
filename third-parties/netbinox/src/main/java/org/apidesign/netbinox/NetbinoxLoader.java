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
import java.security.ProtectionDomain;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader;
import org.osgi.framework.FrameworkEvent;

/** Classloader that eliminates some unnecessary disk touches.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class NetbinoxLoader extends DefaultClassLoader {
    public NetbinoxLoader(ClassLoader parent, ClassLoaderDelegate delegate, ProtectionDomain domain, BaseData bd, String[] classpath) {
        super(parent, delegate, domain, bd, classpath);
        this.manager = new NoTouchCPM(bd, classpath, this);
    }

    @Override
    public String toString() {
        return "NetbinoxLoader delegating to " + delegate;
    }

    @Override
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        //System.out.println("Class " + name + " loaded " + this.getResource(name));
        return super.loadClass(name, resolve);
    }
    
    
    
    private static class NoTouchCPM extends ClasspathManager {
        public NoTouchCPM(BaseData data, String[] classpath, BaseClassLoader classloader) {
            super(data, classpath, classloader);
        }

        @Override
        public ClasspathEntry getClasspath(String cp, BaseData sourcedata, ProtectionDomain sourcedomain) {
            BundleFile bundlefile = null;
            File file;
            BundleEntry cpEntry = sourcedata.getBundleFile().getEntry(cp);
            // check for internal library directories in a bundle jar file
            if (cpEntry != null && cpEntry.getName().endsWith("/")) //$NON-NLS-1$
            {
                bundlefile = createBundleFile(cp, sourcedata);
            } // check for internal library jars
            else if ((file = sourcedata.getBundleFile().getFile(cp, false)) != null) {
                bundlefile = createBundleFile(file, sourcedata);
            }
            if (bundlefile != null) {
                return createClassPathEntry(bundlefile, sourcedomain, sourcedata);
            }
            return null;
        }

        @Override
        public ClasspathEntry getExternalClassPath(String cp, BaseData sourcedata, ProtectionDomain sourcedomain) {
            File file = new File(cp);
            if (!file.isAbsolute()) {
                return null;
            }
            BundleFile bundlefile = createBundleFile(file, sourcedata);
            if (bundlefile != null) {
                return createClassPathEntry(bundlefile, sourcedomain, sourcedata);
            }
            return null;
        }

        private static BundleFile createBundleFile(Object content, BaseData sourcedata) {
            try {
                return sourcedata.getAdaptor().createBundleFile(content, sourcedata);
            } catch (IOException e) {
                sourcedata.getAdaptor().getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR, sourcedata.getBundle(), e);
            }
            return null;
        }
        private ClasspathEntry createClassPathEntry(BundleFile bundlefile, ProtectionDomain cpDomain, final BaseData data) {
            return new ClasspathEntry(bundlefile, createProtectionDomain(bundlefile, cpDomain)) {
                @Override
                public BaseData getBaseData() {
                    return data;
                }
            };
        }
    } // end of NoTouchCPM
}
