package org.openmole.misc.workspace.internal;

import java.io.File;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.openmole.commons.tools.io.FileUtil;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.workspace.IWorkspace;

public class Activator implements BundleActivator {

    private static String simExplorerDir = ".openmole";
    private ServiceRegistration reg;
    private static BundleContext context;
    private IWorkspace workspace;

    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        Logger logger = Logger.getLogger("org.apache.commons.configuration.ConfigurationUtils");
        logger.addAppender(new ConsoleAppender(new PatternLayout("%-5p %d  %c - %F:%L - %m%n")));
        logger.setLevel(Level.WARN);
        workspace = new Workspace(new File(System.getProperty("user.home"), simExplorerDir));
        reg = context.registerService(IWorkspace.class.getName(), workspace, null);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                 FileUtil.recursiveDelete(workspace.getLocation());
            }
            
        });
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        reg.unregister();       
        FileUtil.recursiveDelete(workspace.getLocation());
        context = null;
    }

    public static BundleContext getContext() {
        return context;
    }
}
