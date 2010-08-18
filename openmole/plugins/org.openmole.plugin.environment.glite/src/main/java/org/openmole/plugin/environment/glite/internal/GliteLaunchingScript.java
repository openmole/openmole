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
package org.openmole.plugin.environment.glite.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.core.model.file.IURIFile;
import org.openmole.plugin.environment.glite.GliteEnvironment;
import org.openmole.plugin.environment.jsaga.IJSAGALaunchingScript;

public class GliteLaunchingScript implements IJSAGALaunchingScript {

    final static String ConfigGroup = GliteLaunchingScript.class.getSimpleName();
    final static ConfigurationLocation LCGCPTimeOut = new ConfigurationLocation(ConfigGroup, "RuntimeCopyOnWNTimeOut");

    final GliteEnvironment env;

    static {
        Activator.getWorkspace().addToConfigurations(LCGCPTimeOut, "PT2M");
    }

    public GliteLaunchingScript(GliteEnvironment env) {
        super();
        this.env = env;
    }

    @Override
    public void generateScript(String in, String out, IRuntime runtime, int memorySizeForRuntime, OutputStream os) throws IOException, InternalProcessingError {
        
        PrintStream writter = new PrintStream(os);
       
        writter.print("BASEPATH=$PWD;CUR=$PWD/ws$RANDOM;while test -e $CUR; do CUR=$PWD/ws$RANDOM;done;mkdir $CUR; export HOME=$CUR; cd $CUR; ");
        writter.print(mkLcgCpGunZipCmd(env, runtime.getRuntime().getLocationString(), "$PWD/openmole.tar.bz2"));
        writter.print("mkdir envplugins; PLUGIN=0;");

        for(IURIFile plugin: runtime.getEnvironmentPlugins()) {
            writter.print(mkLcgCpGunZipCmd(env, plugin.getLocationString(), "$CUR/envplugins/plugin$PLUGIN.jar"));
            writter.print("PLUGIN=`expr $PLUGIN + 1`; ");
        }

        writter.print(mkLcgCpGunZipCmd(env, runtime.getEnvironmentAuthenticationFile().getLocationString(),"$CUR/authentication.xml"));

        writter.print("tar -xjf openmole.tar.bz2 >/dev/null; rm -f openmole.tar.bz2; cd org.openmole.runtime-*; export PATH=$PWD/jre/bin:$PATH; /bin/sh run.sh ");
        writter.print(Integer.toString(memorySizeForRuntime));
        writter.print("m ");
        writter.print("-a $CUR/authentication.xml ");
        writter.print("-p $CUR/envplugins/ ");
        writter.print("-i ");
        writter.print(in);
        writter.print(" -o ");
        writter.print(out);
        writter.print(" -w $CUR ; cd .. ; rm -rf $CUR");
    }

    String mkLcgCpGunZipCmd(GliteEnvironment env, String from, String to) throws InternalProcessingError {
        StringBuilder builder = new StringBuilder();

        builder.append("lcg-cp --vo ");
        builder.append(env.getVOName());
        builder.append(" --connect-timeout ");
        builder.append(getTimeOut());
        builder.append(" ");
        builder.append(" --sendreceive-timeout ");
        builder.append(getTimeOut());
        builder.append(" ");
        builder.append(" --bdii-timeout ");
        builder.append(getTimeOut());
        builder.append(" ");
        builder.append(" --srm-timeout ");
        builder.append(getTimeOut());
        builder.append(" ");
        builder.append(from);
        builder.append(" file:");
        builder.append(to);
        builder.append(".gz; gunzip ");
        builder.append(to);
        builder.append(".gz;");

        return builder.toString();
    }


    @SoftCachable
    String getTimeOut() throws InternalProcessingError {
        String timeOut = new Integer(Activator.getWorkspace().getPreferenceAsDurationInS(LCGCPTimeOut)).toString();
        return timeOut;
    }
}
