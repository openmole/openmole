/*
 *  Copyright (C) 2009-2010 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; version 2
 *  of the License.
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
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;
import org.eclipse.osgi.framework.internal.core.PackageAdminImpl;
import org.eclipse.osgi.launch.Equinox;
import org.openide.util.Lookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
class Netbinox extends Equinox {

    public Netbinox(Map configuration) {
        super(configuration);
    }

    @Override
    public BundleContext getBundleContext() {
        return new Context(super.getBundleContext());
    }

    private static final class Context implements BundleContext {

        private final BundleContext delegate;

        public Context(BundleContext delegate) {
            this.delegate = delegate;
        }

        public boolean ungetService(ServiceReference sr) {
            return delegate.ungetService(sr);
        }

        public void removeServiceListener(ServiceListener sl) {
            delegate.removeServiceListener(sl);
        }

        public void removeFrameworkListener(FrameworkListener fl) {
            delegate.removeFrameworkListener(fl);
        }

        public void removeBundleListener(BundleListener bl) {
            delegate.removeBundleListener(bl);
        }

        public ServiceRegistration registerService(String string, Object o, Dictionary dctnr) {
            return delegate.registerService(string, o, dctnr);
        }

        public ServiceRegistration registerService(String[] strings, Object o, Dictionary dctnr) {
            return delegate.registerService(strings, o, dctnr);
        }

        public Bundle installBundle(String string) throws BundleException {
            return installBundle(string, null);
        }

        public Bundle installBundle(String url, InputStream in) throws BundleException {
            if (url.startsWith("reference:")) {
                // workaround for problems with space in path
                url = url.replaceAll("%20", " ");
            }
            return delegate.installBundle(url, in);
        }

        public ServiceReference[] getServiceReferences(String string, String string1) throws InvalidSyntaxException {
            return delegate.getServiceReferences(string, string1);
        }

        public ServiceReference getServiceReference(String string) {
            return delegate.getServiceReference(string);
        }

        public Object getService(ServiceReference sr) {
            return delegate.getService(sr);
        }

        public String getProperty(String string) {
            return delegate.getProperty(string);
        }

        public File getDataFile(String string) {
            return delegate.getDataFile(string);
        }

        public Bundle[] getBundles() {
            return delegate.getBundles();
        }

        public Bundle getBundle(long l) {
            return delegate.getBundle(l);
        }

        public Bundle getBundle() {
            return delegate.getBundle();
        }

        public ServiceReference[] getAllServiceReferences(String string, String string1) throws InvalidSyntaxException {
            return delegate.getAllServiceReferences(string, string1);
        }

        public Filter createFilter(String string) throws InvalidSyntaxException {
            return delegate.createFilter(string);
        }

        public void addServiceListener(ServiceListener sl) {
            delegate.addServiceListener(sl);
        }

        public void addServiceListener(ServiceListener sl, String string) throws InvalidSyntaxException {
            delegate.addServiceListener(sl, string);
        }

        public void addFrameworkListener(FrameworkListener fl) {
            delegate.addFrameworkListener(fl);
        }

        public void addBundleListener(BundleListener bl) {
            delegate.addBundleListener(bl);
        }
    } // end of Context
}
