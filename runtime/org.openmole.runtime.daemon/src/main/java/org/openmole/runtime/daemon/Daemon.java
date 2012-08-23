/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.runtime.daemon;

import org.openmole.core.batch.authentication.Authentication;
import java.io.File;
import java.util.logging.Level;

import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.openmole.core.batch.file.URIFile;
import org.openmole.core.batch.authentication.JSAGASessionService;
import org.openmole.misc.pluginmanager.PluginManager;
import org.openmole.misc.workspace.Workspace;
import org.openmole.core.serializer.SerializerService;

public class Daemon implements IApplication {
    
    @Override
    public Object start(IApplicationContext context) throws Exception {
        try {
            System.setProperty("org.openmole.misc.workspace.noUniqueResource", "true");
            String args[] = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
            
            Options options = new Options();
            
            options.addOption("h", true, "user@hostname:port");
            options.addOption("p", true, "password");
            options.addOption("w", true, "number of workers");
            options.addOption("d", false, "debug mode");
            options.addOption("c", true, "cache size in Mo (default is 2000)");
            
            CommandLineParser parser = new BasicParser();
            CommandLine cmdLine;
            
            try {
                cmdLine = parser.parse(options, args);
            } catch (ParseException e) {
                Logger.getLogger(Daemon.class.getName()).severe("Error while parsing command line arguments");
                new HelpFormatter().printHelp(" ", options);
                return IApplication.EXIT_OK;
            }
            
            String userHostnamePort = cmdLine.getOptionValue("h");
            String password = cmdLine.getOptionValue("p");
            boolean debug = cmdLine.hasOption("d");
            
            int workers = 1;
            
            if(cmdLine.hasOption("w")) workers = new Integer(cmdLine.getOptionValue("w"));
            
            if (userHostnamePort == null || password == null) {
                Logger.getLogger(Daemon.class.getName()).severe("Error while parsing command line arguments");
                new HelpFormatter().printHelp(" ", options);
                return IApplication.EXIT_OK;                
            }
            
            long cacheSize = 2000;
            if(cmdLine.hasOption("c")) cacheSize = new Long(cmdLine.getOptionValue("c"));
            
            new JobLauncher(cacheSize * 1024 * 1024, debug).launch(userHostnamePort, password, workers);
            
        } catch (Throwable t) {
            Logger.getLogger(Daemon.class.getName()).log(Level.SEVERE, "Error durring runtime execution", t);
        }
        return IApplication.EXIT_OK;
    }
    
    @Override
    public void stop() {
    }
}
