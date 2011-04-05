/*
 *  Copyright (C) 2009 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.netbeans.core.netigso.spi.NetigsoArchive;
import org.openide.util.lookup.ServiceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.packageadmin.PackageAdmin;
/**
 *
 * @author Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 */
@ServiceProvider(
    service=FrameworkFactory.class,
    supersedes={ "org.eclipse.osgi.launch.EquinoxFactory" },
    position=-10
)
public class NetbinoxFactory implements FrameworkFactory {
    static final Logger LOG = Logger.getLogger("org.apidesign.netbinox"); // NOI18N

    @SuppressWarnings("unchecked")
    public Framework newFramework(Map map) {
        Map<String,Object> configMap = new HashMap<String,Object>();
        configMap.putAll(map);
        configMap.put("osgi.hook.configurators.exclude", // NOI18N
            "org.eclipse.core.runtime.internal.adaptor.EclipseLogHook" // NOI18N
//            + ",org.eclipse.core.runtime.internal.adaptor.EclipseClassLoadingHook" // NOI18N
        );
        configMap.put("osgi.hook.configurators.include", NetbinoxHooks.class.getName()); // NOI18N
        configMap.put("osgi.user.area.default", configMap.get(Constants.FRAMEWORK_STORAGE)); // NOI18N
        configMap.put("osgi.instance.area.default", System.getProperty("netbeans.user")); // NOI18N
        configMap.put("osgi.install.area", System.getProperty("netbeans.home")); // NOI18N
        // some useless value
        configMap.put("osgi.framework.properties", System.getProperty("netbeans.user")); // NOI18N

        Object rawBundleMap = configMap.get("felix.bootdelegation.classloaders"); // NOI18N

        Map<Bundle,ClassLoader> bundleMap;
        if (rawBundleMap == null) {
            bundleMap = null;
        } else {
            bundleMap = (Map<Bundle,ClassLoader>)rawBundleMap;
        }

        NetbinoxHooks.registerMap(bundleMap);
        NetbinoxHooks.registerArchive((NetigsoArchive)configMap.get("netigso.archive")); // NOI18N

        String loc = EquinoxFactory.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
        int file = loc.indexOf("file:");
        if (file > 0) {
            loc = loc.substring(file);
        }
        int exclaim = loc.indexOf("!");
        if (exclaim > 0) {
            loc = loc.substring(0, exclaim);
        }
        configMap.put("osgi.framework", loc);
        Netbinox ret = new Netbinox(configMap);
       /* try {
            ret.init();
        } catch (BundleException ex) {
            throw new RuntimeException("Error initializing equinox.", ex);
        }*/
        
        return ret;
    }
}
