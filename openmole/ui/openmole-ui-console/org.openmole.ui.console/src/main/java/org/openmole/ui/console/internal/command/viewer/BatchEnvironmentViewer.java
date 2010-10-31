/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.ui.console.internal.command.viewer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.IExecutionJobRegistry;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.model.execution.batch.IBatchJob;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;

import scala.collection.Iterator;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class BatchEnvironmentViewer implements IViewer<IBatchEnvironment<?>> {

    EnvironmentViewer environmentViewer = new EnvironmentViewer();

    @Override
    public void view(IBatchEnvironment<?> object, String[] args) {
        Option verbosity = OptionBuilder.withLongOpt("verbose").withDescription("level of verbosity").hasArgs(1).withArgName("level").withType(Integer.class).isRequired(false).create("v");
        Options options = new Options().addOption(verbosity);

        try {
            CommandLineParser parser = new BasicParser();
            CommandLine commandLine = parser.parse(options, args);

            Integer v = 0;

            if (commandLine.hasOption(verbosity.getOpt())) {
                v = new Integer(commandLine.getOptionValue(verbosity.getOpt()));
            }

            environmentViewer.view(object, args);
            if (v >= 1) {
                System.out.println(Separator);
                Map<IBatchServiceDescription, Map<ExecutionState, AtomicInteger>> jobServices = new HashMap<IBatchServiceDescription, Map<ExecutionState, AtomicInteger>>();
                IExecutionJobRegistry<? extends IBatchExecutionJob> executionJobRegistry = object.jobRegistry();
                Iterator<? extends IBatchExecutionJob> it = executionJobRegistry.getAllExecutionJobs().iterator();
                while (it.hasNext()) {
                    IBatchExecutionJob executionJob = it.next();
                    IBatchJob batchJob = executionJob.batchJob();
                    if (batchJob != null) {
                        Map<ExecutionState, AtomicInteger> jobServiceInfo = jobServices.get(batchJob.jobServiceDescription());
                        if (jobServiceInfo == null) {
                            jobServiceInfo = new EnumMap<ExecutionState, AtomicInteger>(ExecutionState.class);
                            jobServices.put(batchJob.jobServiceDescription(), jobServiceInfo);
                        }
                        AtomicInteger nb = jobServiceInfo.get(batchJob.state());
                        if (nb == null) {
                            nb = new AtomicInteger();
                            jobServiceInfo.put(batchJob.state(), nb);
                        }
                        nb.incrementAndGet();
                    }
                }


                for (IBatchServiceDescription description : jobServices.keySet()) {
                    System.out.print(description.toString() + ":");
                    Map<ExecutionState, AtomicInteger> jobServiceInfo = jobServices.get(description);
                    for (ExecutionState state : jobServiceInfo.keySet()) {
                        System.out.print(" [" + state.name() + " = " + jobServiceInfo.get(state).get() + "]");
                    }
                    System.out.println();
                }
            }

            if (v >= 2) {
                System.out.println(Separator);

                IExecutionJobRegistry<? extends IBatchExecutionJob> executionJobRegistry = object.jobRegistry();
                Iterator<? extends IBatchExecutionJob> it = executionJobRegistry.getAllExecutionJobs().iterator();
                while (it.hasNext()) {
                    IBatchExecutionJob executionJob = it.next();
                    IBatchJob batchJob = executionJob.batchJob();
                    if (batchJob != null) {
                        System.out.println(batchJob.toString() + " " + batchJob.state().toString());
                    }
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(BatchEnvironmentViewer.class.getName()).log(Level.SEVERE, "Wrong arguments format.");
            new HelpFormatter().printHelp(" ", options);
        }

    }
}
