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
import org.openmole.ui.console.internal.Activator;
import org.openmole.ui.console.internal.Console;
import org.openmole.ui.console.internal.command.Init;
import org.openmole.ui.console.internal.command.Print;
import org.openmole.misc.workspace.IWorkspace$;
/**
 * Hello world!
 *
 */
public class Application implements IApplication {

    final public static String pluginManager = "plugin";
    final public static String structureGenerator = "structure";
    final public static String workspace = "workspace";
    final public static String registry = "registry";

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

        
        File workspaceLocation = IWorkspace$.MODULE$.defaultLocation();
        if (cmd.hasOption(workspaceDir.getOpt())) {
            workspaceLocation = new File(cmd.getOptionValue(workspaceDir.getOpt()));
        }

        if(IWorkspace$.MODULE$.isAlreadyRunningAt(workspaceLocation)) {
            Logger.getLogger(Application.class.getName()).severe("Application is already runnig at " + workspaceLocation.getAbsolutePath()+ ". If it is not the case please remove the file '" + IWorkspace$.MODULE$.running() + "'.");
            return IApplication.EXIT_OK;
        }
        
        Activator.getWorkspace().location_$eq(workspaceLocation);
        
        // Init Console
        Console console = Activator.getConsole();
        console.setVariable(pluginManager, Activator.getPluginManager());
        console.setVariable(structureGenerator, Activator.getStructureGenerator());
        console.setVariable(workspace, Activator.getWorkspace());

        Groovysh g = console.getGroovysh();
        Groovysh muteShell = console.getMuteGroovysh();
        g.leftShift(new Print(g, "print", "\\pr"));
        g.leftShift(new Init(g, muteShell, "init", "\\in"));

        console.run("init " + workspace);
        
        // Process CLI options
        if (cmd.hasOption(optionPluginsDir.getOpt())) {
            for (String directory : cmd.getOptionValue(optionPluginsDir.getOpt()).split(",")) {
                Activator.getPluginManager().loadDir(directory);
            }
        }
        
        // Run
        console.getGroovysh().run();
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
    }
}
