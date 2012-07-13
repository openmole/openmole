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
import org.openmole.misc.pluginmanager.PluginManager;
import org.openmole.misc.workspace.Workspace;
import org.openmole.core.serializer.SerializerService;
import org.openmole.misc.logging.LoggerService;

public class SimExplorer implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        try {
            String args[] = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

            Options options = new Options();

            options.addOption("a", true, "Path to a serialized authentication to initialize.");
            options.addOption("s", true, "Base uri for the storage.");
            options.addOption("i", true, "URI of the input message.");
            options.addOption("o", true, "URI of the output message.");
            options.addOption("c", true, "Path for the communication.");
            options.addOption("p", true, "Path for plugin dir to preload.");
            options.addOption("l", true, "Local authentication mode for debug.");
            options.addOption("d", false, "Debug mode.");

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

            String environmentPluginDirPath = cmdLine.getOptionValue("p");
            String executionMessageURI = cmdLine.getOptionValue("i");
            String baseURI = cmdLine.getOptionValue("s");
            String communicationPath = cmdLine.getOptionValue("c");

            //System.out.println("plugin path " + environmentPluginDirPath);
            File environmentPluginDir = new File(environmentPluginDirPath);
            PluginManager.loadDir(environmentPluginDir);

            if (cmdLine.hasOption("l")) {
                Workspace.instance().password_$eq(cmdLine.getOptionValue("l"));
                debug = true;
            }

            if (cmdLine.hasOption("d")) {
                debug = true;
            }

            if (debug) {
                LoggerService.level("ALL");
            }

            //if (cmdLine.hasOption("a")) {
                /*
             * get env and init
             */

            Authentication authentication = null;
            if (cmdLine.hasOption("a")) {
                File envFile = new File(cmdLine.getOptionValue("a"));
                authentication = SerializerService.deserializeAndExtractFiles(envFile);
                authentication.initialize(false);
            }
            //if(!debug) envFile.delete();
            //}


            String outMesseageURI = cmdLine.getOptionValue("o");

            new Runtime().apply(baseURI, communicationPath, executionMessageURI, outMesseageURI, debug);

            //Be sure it is not garbage collected
            if (authentication != null) {
                authentication.getClass();
            }

        } catch (Throwable t) {
            Logger.getLogger(SimExplorer.class.getName()).log(Level.SEVERE, "Error durring runtime execution", t);
        }
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }
}
