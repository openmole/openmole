/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.misc.pluginmanager.internal;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.ExportedPackage;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.pluginmanager.IPluginManager;

public class PluginManager implements IPluginManager {

    final String defaultPatern = ".*\\.jar";
    BidiMap<Bundle, File> files = new DualHashBidiMap<Bundle, File>();
    Map<Bundle, Iterable<Bundle>> resolvedBundlesDirectDependencies = new HashMap<Bundle, Iterable<Bundle>>();
    Map<Bundle, Iterable<Bundle>> resolvedBundlesAllDependencies = new HashMap<Bundle, Iterable<Bundle>>();

    @Override
    public synchronized Bundle load(File path) throws InternalProcessingError {
        try {
            Bundle b = installBundle(path);

            if (b != null) {                
                if (!resolvedBundlesDirectDependencies.containsKey(b)) {
                    b.start();
                    resolvedBundlesDirectDependencies.put(b, getDirectDependencieBundles(b));
                }
            } else {
                b = getBundleForFile(path);
            }
            return b;
        } catch (BundleException ex) {
            throw new InternalProcessingError(ex, "Installing " + path.getAbsolutePath() + " " + ex.getLocalizedMessage());
        }
    }

    @Override
    public List<Bundle> loadDir(File path, final String pattern) throws InternalProcessingError {
        return loadDir(path, new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.exists() && file.getName().matches(pattern);
            }
        });
    }

    @Override
    public synchronized List<Bundle> loadDir(File path, FileFilter fiter) throws InternalProcessingError {
        if (!path.exists() || !path.isDirectory()) {
            return Collections.EMPTY_LIST;
        }

        try {
            List<Bundle> bundlesRet = new LinkedList<Bundle>();
            List<Bundle> bundles = new LinkedList<Bundle>();

            for (File f : path.listFiles(fiter)) {
                Bundle b;

                b = installBundle(f);

                if (b != null && !resolvedBundlesDirectDependencies.containsKey(b)) {
                    bundles.add(b);
                    
                    resolvedBundlesDirectDependencies.put(b, Collections.EMPTY_LIST);
                }
                
                if (b != null) {
                    bundlesRet.add(b);
                    
                } else {
                    bundlesRet.add(getBundleForFile(f));
                    
                }
            }


            for (Bundle b : bundles) {
                try {
                    b.start();
                } catch (BundleException ex) {
                    throw new InternalProcessingError(ex, "Installing " + b.getLocation() + " " + ex.getLocalizedMessage());
                }
            }

            for (Bundle b : bundles) {
                resolvedBundlesDirectDependencies.put(b, getDirectDependencieBundles(b));
            }

            return bundlesRet;

        } catch (BundleException ex) {
            throw new InternalProcessingError(ex, ex.getLocalizedMessage());
        }
    }

    Set<Bundle> getDirectDependencieBundles(Bundle b) {
        Set<Bundle> dependencies = new HashSet<Bundle>();

        //TODO find a way to do that faster
        for (Bundle bundle : resolvedBundlesDirectDependencies.keySet()) {
            for (Bundle ib : getDependingBundles(bundle)) {
                if (ib.equals(b)) {
                    dependencies.add(bundle);
                }
            }
        }

        return dependencies;
    }

    public synchronized Iterable<Bundle> getResolvedDirectDependencies(Bundle b) {
        if (resolvedBundlesDirectDependencies.containsKey(b)) {
            return resolvedBundlesDirectDependencies.get(b);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public synchronized Iterable<Bundle> getAllDependencies(Bundle b) {
        if (resolvedBundlesAllDependencies.containsKey(b)) {
            return resolvedBundlesAllDependencies.get(b);
        } else {
            Collection<Bundle> deps = new HashSet<Bundle>();
            LinkedList<Bundle> toProced = new LinkedList<Bundle>();

            toProced.push(b);

            while (!toProced.isEmpty()) {
                Bundle cur = toProced.pop();

                for (Bundle dep : getResolvedDirectDependencies(cur)) {
                    if (!deps.contains(dep)) {
                        deps.add(dep);
                        toProced.push(dep);
                    }
                }
            }
            resolvedBundlesAllDependencies.put(b, deps);
            return deps;
        }
    }

    @Override
    public synchronized File getPluginForClass(Class c) {
        Bundle b = Activator.getPackageAdmin().getBundle(c);
        return files.get(b);
    }

    @Override
    public synchronized Iterable<File> getPluginAndDependanciesForClass(Class c) {
        Bundle b = Activator.getPackageAdmin().getBundle(c);

        if (b == null) {
            return Collections.EMPTY_LIST;
        }

        Collection<File> ret = new LinkedList<File>();


        File plugin = files.get(b);
        if (plugin != null) {
            ret.add(plugin);
        }

        for (Bundle dep : getAllDependencies(b)) {
            File depPlugin = files.get(dep);
            if (depPlugin != null) {
                ret.add(depPlugin);
            }
        }

        return ret;
    }

    @Override
    public Bundle load(String path) throws InternalProcessingError {
        return load(new File(path));
    }

    @Override
    public boolean isClassProvidedByAPlugin(Class c) {
        boolean ret = resolvedBundlesDirectDependencies.containsKey(Activator.getPackageAdmin().getBundle(c));
        return ret;
    }

    @Override
    public synchronized void unload(File path) throws InternalProcessingError {
        Bundle b = getBundle(path);

        for (Bundle db : getDependingBundles(b)) {
            unloadBundle(db);
        }

        unloadBundle(b);
    }

    private void unloadBundle(Bundle bundle) throws InternalProcessingError {
        if (bundle != null) {
            try {
                bundle.uninstall();
                files.remove(bundle);
                resolvedBundlesDirectDependencies.remove(bundle);
            } catch (BundleException ex) {
                throw new InternalProcessingError(ex);
            }
        }
    }

    @Override
    public synchronized Bundle getBundle(File path) throws InternalProcessingError {
        return files.getKey(path);
    }

    private Iterable<Bundle> getDependingBundles(Bundle b) {
        Collection<Bundle> ret = new LinkedList<Bundle>();

        ExportedPackage[] exportedPackages = Activator.getPackageAdmin().getExportedPackages(b);

        if (exportedPackages != null) {
            for (ExportedPackage exportedPackage : exportedPackages) {
                for (Bundle ib : exportedPackage.getImportingBundles()) {
                    ret.add(ib);
                }
            }
        }

        return ret;
    }

    @Override
    public void unload(String path) throws InternalProcessingError {
        unload(new File(path));
    }

    @Override
    public Collection<Bundle> loadDir(File path) throws InternalProcessingError {
        return loadDir(path, defaultPatern);
    }

    @Override
    public Collection<Bundle> loadDir(String path) throws InternalProcessingError {
        return loadDir(new File(path));
    }
    
    private Bundle getBundleForFile(File f) {
        return files.getKey(f.getAbsoluteFile());
    }
    
    private Bundle installBundle(File f) throws BundleException {
        File file = f.getAbsoluteFile();
        
        if (!files.containsValue(file)) {
            Bundle ret = Activator.getContext().installBundle(file.toURI().toString());
            files.put(ret, file);
            return ret;
        }
        return null;
    }
}
