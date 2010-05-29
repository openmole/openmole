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
package org.openmole.plugin.environmentprovider.glite.internal;

import org.openmole.core.implementation.execution.batch.BatchEnvironment;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.workflow.model.execution.batch.IRuntime;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.plugin.environmentprovider.glite.GliteEnvironment;
import org.openmole.plugin.environmentprovider.jsaga.model.IJSAGALaunchingScript;

public class GliteLaunchingScript implements IJSAGALaunchingScript<GliteEnvironment> {

    final static String ConfigGroup = GliteLaunchingScript.class.getSimpleName();
    final static ConfigurationLocation LCGCPTimeOut = new ConfigurationLocation(ConfigGroup, "RuntimeCopyOnWNTimeOut");

    final Integer memorySize;

    static {
        Activator.getWorkspace().addToConfigurations(LCGCPTimeOut, "PT30M");
    }

    public GliteLaunchingScript(Integer memorySize) {
        super();
        this.memorySize = memorySize;
    }

    @Override
    public String getScript(String args, IRuntime runtime,  GliteEnvironment env) throws InternalProcessingError {

        StringBuilder builder = new StringBuilder();

        builder.append("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; ");
        builder.append(mkLcgCpCmd(env, runtime.getRuntime().getLocationString(), "$PWD/simexplorer.tar.bz2"));
        builder.append("mkdir envplugins; PLUGIN=0;");

        for(IURIFile plugin: runtime.getEnvironmentPlugins()) {
            builder.append(mkLcgCpCmd(env, plugin.getLocationString(), "$CUR/envplugins/plugin$PLUGIN.jar"));
            builder.append("PLUGIN=`expr $PLUGIN + 1`; ");
        }

        builder.append(mkLcgCpCmd(env, runtime.getEnvironmentDescriptionFile().getLocationString(),"$CUR/envinronmentDescription.xml"));

        builder.append("tar -xjf simexplorer.tar.bz2 >/dev/null; rm -f simexplorer.tar.bz2; cd org.openmole.runtime-*; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ");
        builder.append(Activator.getWorkspace().getPreference(BatchEnvironment.MemorySizeForRuntime));
        builder.append("m ");
        builder.append("$CUR/envinronmentDescription.xml ");
        builder.append("$CUR/envplugins/ ");
        builder.append(' ');
        builder.append(args);
        builder.append(" $CUR; cd .. ; rm -rf $CUR");

        String script = builder.toString();

        //System.out.println(script);

        return script;
    }

    String mkLcgCpCmd(GliteEnvironment env, String from, String to) throws InternalProcessingError {
        StringBuilder builder = new StringBuilder();

        builder.append("lcg-cp --vo ");
        builder.append(env.getVoName());
        builder.append(" -t ");
        builder.append(getTimeOut());
        builder.append(" ");
        builder.append(from);
        builder.append(" file:");
        builder.append(to);
        builder.append("; ");

        return builder.toString();
    }


    @SoftCachable
    String getTimeOut() throws InternalProcessingError {
        String timeOut = new Integer(Activator.getWorkspace().getPreferenceAsDurationInS(LCGCPTimeOut)).toString();
        return timeOut;
    }
}
