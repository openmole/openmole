/*
 *
 *  Copyright (c) 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ui.ide.workflow.model;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openmole.commons.exception.UserBadDataError;

public class ExplorationsManager {

    private static ExplorationsManager instance;
    private XStream xstream;
    private ExecutorTask explorationTask;
    private InputOutput inputOutput;
    private static final Logger logger = Logger.getLogger(ExplorationsManager.class.getName());

    public ExplorationsManager() {
        xstream = new XStream();
    }

    public static ExplorationsManager getInstance() {
        if (instance == null) {
            instance = new ExplorationsManager();
        }
        return instance;
    }

    public static void saveApplication(ExplorationApplication application, File file) throws IOException {
        getInstance().xstream.toXML(application, new java.io.FileWriter(file));
        application.setSavedAs(file);
    }

    public static void saveApplication(ExplorationApplication application) throws IOException {
        saveApplication(application, application.getFileSaved());
    }

    public static ExplorationApplication loadApplication(File file) throws FileNotFoundException {
        return (ExplorationApplication) getInstance().xstream.fromXML(new FileReader(file));
    }

    public static void run(final ExplorationApplication explorationApplication) {
        getInstance().inputOutput = IOProvider.getDefault().getIO("Output", false);
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    getInstance().inputOutput.getOut().reset();
                    // work on a copy of the application
                    getInstance().inputOutput.getOut().println("Exploration starting…");
                    logger.info("Exploration starting…");
                    ExplorationApplication ea = explorationApplication.copy();
                    ea.buildWorkflow().run();
                    getInstance().inputOutput.getOut().println("Exploration completed!");
                    logger.info("Exploration completed !");
                } catch (UserBadDataError ex) {
                    getInstance().inputOutput.getOut().println("Exploration interrupted because of errors");
                    if (ex.getMessage() != null) {
                        getInstance().inputOutput.getOut().println(ex.getMessage());
                    }
                    logger.log(Level.INFO, "Exploration interrupted because of errors", ex);
                } catch (Exception ex) {
                    getInstance().inputOutput.getOut().println("Exploration interrupted because of a system error." + " You can report a bug with your log file (accessible through the menu \"View -> IDE log\")" + " and your saved application.");
                    if (ex.getMessage() != null) {
                        getInstance().inputOutput.getOut().println(ex.getMessage());
                    }
                    getInstance().inputOutput.getOut().println(" At " + ex.getStackTrace()[0]);
                    logger.log(Level.INFO, "Exploration interrupted because of errors", ex);
                }
            }
        };
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                getInstance().explorationTask = ExecutionEngine.getDefault().execute("Exploration", runnable, getInstance().inputOutput);
            }
        });
    }

    public static void stop() {
        getInstance().inputOutput.getOut().println("Exploration stopped.");
        getInstance().explorationTask.stop();
    }
}
