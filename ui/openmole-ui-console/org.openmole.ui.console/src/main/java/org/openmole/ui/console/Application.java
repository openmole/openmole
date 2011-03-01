package org.openmole.ui.console;

import java.io.File;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.openmole.core.structuregenerator.StructureGenerator;
import org.openmole.misc.pluginmanager.PluginManager;
import org.openmole.misc.workspace.Workspace;
import org.openmole.ui.console.internal.command.Init;
import org.openmole.ui.console.internal.command.Print;


public class Application implements IApplication {


    @Override
    public Object start(IApplicationContext context) throws Exception {

        // Init options parsing
        String[] args = (String[]) context.getArguments().get("application.args");
        Option optionPluginsDir = OptionBuilder.withLongOpt("pluginsDir").withDescription("Add plugins directories (seperated by \",\")").hasArgs(1).withArgName("directories").isRequired(false).create("p");
        Option workspaceDir = OptionBuilder.withLongOpt("workspaceDir").withDescription("Directory of the workspace").hasArgs(1).withArgName("directory").isRequired(false).create("w");

        Options options = new Options().addOption(optionPluginsDir).addOption(workspaceDir);
        CommandLineParser parser = new BasicParser();
        
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            Logger.getLogger(Application.class.getName()).severe("Error while parsing command line arguments");
            new HelpFormatter().printHelp(" ", options);
            return IApplication.EXIT_OK;
        }

        
        File workspaceLocation = Workspace.defaultLocation();
        if (cmd.hasOption(workspaceDir.getOpt())) {
            workspaceLocation = new File(cmd.getOptionValue(workspaceDir.getOpt()));
        }

        if(Workspace.isAlreadyRunningAt(workspaceLocation)) {
            Logger.getLogger(Application.class.getName()).severe("Application is already runnig at " + workspaceLocation.getAbsolutePath()+ ". If it is not the case please remove the file '" + new File(workspaceLocation, Workspace.running()).getAbsolutePath() + "'.");
            return IApplication.EXIT_OK;
        }

        if(cmd.hasOption(workspaceDir.getOpt())) Workspace.instance_$eq(new Workspace(workspaceLocation));

        
        Groovysh g = Console.groovysh();
        Groovysh muteShell = Console.muteGroovysh();
        g.leftShift(new Print(g, "print", "\\pr"));
        g.leftShift(new Init(g, muteShell, "init", "\\in"));

        Console.run("init " + Console.workspace());
        
        // Process CLI options
        if (cmd.hasOption(optionPluginsDir.getOpt())) {
            for (String directory : cmd.getOptionValue(optionPluginsDir.getOpt()).split(",")) {
                PluginManager.loadDir(directory);
            }
        }
        
        // Run
        Console.groovysh().run();
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }
}
