/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.runtime;

import org.openmole.core.batch.environment.BatchAuthentication;
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
import org.openmole.core.batch.jsaga.JSAGASessionService;
import org.openmole.misc.pluginmanager.PluginManager;
import org.openmole.misc.workspace.Workspace;
import org.openmole.core.serializer.SerializerService;

public class SimExplorer implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        try {
            String args[] = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

            Options options = new Options();

            options.addOption("a", true, "Path to a serialized authentication to initialize.");
            options.addOption("w", true, "Path for the workspace.");
            options.addOption("i", true, "Path for the input message.");
            options.addOption("o", true, "Path for the output message.");
            options.addOption("p", true, "Path for plugin dir to preload.");
            options.addOption("l", true, "Local authentication mode for debug.");

            CommandLineParser parser = new BasicParser();
            CommandLine cmdLine;

            boolean debug = false;
            
            try {
                cmdLine = parser.parse(options, args);
            } catch (ParseException e) {
                Logger.getLogger(SimExplorer.class.getName()).severe("Error while parsing command line arguments");
                new HelpFormatter().printHelp(" ", options);
                return IApplication.EXIT_OK;
            }

            Workspace.instance_$eq(new Workspace(new File(cmdLine.getOptionValue("w"))));

            //init jsaga
            JSAGASessionService.session();

            String environmentPluginDirPath = cmdLine.getOptionValue("p");
            String executionMessageURI = cmdLine.getOptionValue("i");

            File environmentPluginDir = new File(environmentPluginDirPath);
            PluginManager.loadDir(environmentPluginDir);

            
            if ( cmdLine.hasOption("l") ) {
                Workspace.instance().password_$eq(cmdLine.getOptionValue("l"));
                debug = true;
            }
            
            if (cmdLine.hasOption("a")) {
                /* get env and init */
                File envFile = new File(cmdLine.getOptionValue("a"));
                BatchAuthentication authentication = SerializerService.deserialize(envFile);
                authentication.initialize();
                if(!debug) envFile.delete();
            }


            String outMesseageURI = cmdLine.getOptionValue("o");

            new Runtime().apply(executionMessageURI, outMesseageURI, debug);


        } catch (Throwable t) {
            Logger.getLogger(SimExplorer.class.getName()).log(Level.SEVERE, "Error durring runtime execution", t);
        }
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }
}
