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
 * <p>The purpose of this plugin is to define manually a plan. It means that
 * no particular strategy is used to generate the sets of parameters of the plan.
 * A CSV plan is thus a plan that can be manually created line by line by according
 * to the CSV format.
 *
 * <h3>CSV format</h3>
 * <prototype1>,<prototype2>,...
 * val11,val21,...
 * val21,val22,...
 * val31,val32,...
 * ...
 *
 *
 * <h3>Example</h3>
 * <p>Let us use the following CSV file as CSV plan file:
 *
 * p1,p2,p3
 * 1,test,2.4
 * 2,for,3.1
 * 3,CSV,2.004
 * 4,plan.,6.2
 * 5,Enjoy!,0.4
 *
 * <pre>{@code

 * import org.openmole.plugin.plan.csv.CSVPlan
 * import org.openmole.plugin.task.groovy.GroovyTask
 * import static org.openmole.ui.plugin.transitionfactory.TransitionFactory.*;
 *
 * p1 = builder.buildPrototype("p1", BigInteger.class)
 * p2 = builder.buildPrototype("p2", String.class)
 * p3 = builder.buildPrototype("p3", BigDecimal.class)
 *
 * dataSet = builder.buildDataSet(p1,p2,p3)
 *
 * plan = new CSVPlan("/home/mathieu/Work/OpenMole/scripts/plugin_examples/csvplan.csv")
 * plan.addColumn(p1)
 * plan.addColumn(p2)
 * plan.addColumn(p3)
 *
 * explorationTask = builder.exploration.buildExplorationTask("explorationTask",plan)
 *
 * displayTask = new GroovyTask("displayTask")
 * displayTask.addInput(dataSet)
 * displayTask.setCode("println 'Run '+${p1}+', '+${p2}+', '+${p3}")
 *
 * exploration = buildExploration(explorationTask,
 *                                buildChain(displayTask))
 * builder.buildMole(exploration.getFirstCapsule()).run()
 *
 * </pre>
 */
package org.openmole.plugin.plan.csv;
