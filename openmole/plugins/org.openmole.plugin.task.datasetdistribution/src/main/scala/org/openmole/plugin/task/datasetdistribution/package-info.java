/*
 *  Copyright (C) 2010 mathieu
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
/**
 * <h3>Example</h3>
 * <p>The following example shows how to plot the distribution of two prototypes
 * after exploration.
 *
 * <p>Let us use the following CSV file as CSV plan file:
 * *<pre>
 * Ly,out,fake
 * 200,250,22.3
 * 300,300,11.666
 * 200,352,63.0
 * 200,253,34.0
 * 150,250,63.0
 * 75,10,33.0
 * 90,110,23.0
 * 222,105,44.0
 * 224,123,11.0
 * 200,204,63.0
 * </pre>
 * 
 * <pre>{@code
 * import static org.openmole.ui.plugin.transitionfactory.TransitionFactory.*
 * import org.openmole.plugin.task.datasetdistribution.DatasetDistributionTask
 * import org.openmole.plugin.plan.csv.CSVPlan;
 * import org.openmole.plugin.task.groovy.GroovyTask;
 * import org.openmole.core.implementation.data.Util;

 * workflow = {
 * ly = builder.buildPrototype("Ly", BigDecimal)
 * out = builder.buildPrototype("out", BigDecimal)
 * dataSet = builder.buildDataSet(ly,out)
 *
 * CSVPlan plan = new CSVPlan("/home/mathieu/Work/OpenMole/scripts/plugin_examples/csvchartdata.csv")
 * plan.addColumn(ly)
 * plan.addColumn(out)
 *
 * explorationTask = builder.exploration.buildExplorationTask("exploration", plan);
 *
 * GroovyTask doubleTask = new GroovyTask("doubleTask");
 * doubleTask.setCode("println out")
 * doubleTask.addInput(dataSet)
 * doubleTask.addOutput(dataSet)
 *
 * DatasetDistributionTask chartTask =
 *           new DatasetDistributionTask("chartTask",
 *                                       "/tmp/",
 *                                       "10",
 *                                       "1000",
 *                                       "1200",
 *                                       "test chart",
 *                                       "Ly",
 *                                       "Occurencies",
 *                                       800,
 *                                       800);
 *
 * chartTask.addChart(Util.toArray(out))
 * chartTask.addChart(Util.toArray(ly))
 *
 * strat = builder.buildFixedEnvironmentStrategy()
 *   //strat.setEnvironment(puzzle.getFirstCapsule(),env)
 *
 * return builder.buildMole(buildExploration(explorationTask,
 *                                      build(doubleTask),
 *                                      chartTask),
 *                            strat)
 * }
 * ex = workflow()
 * ex.start()}
 *
 * 2 images (Ly.png and out.png) are generated in /tmp.
 */
package org.openmole.plugin.task.datasetdistribution;
