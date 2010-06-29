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
 * <p> The CompletePlan enables to generate an exhaustive design of experiment from
 * a set of parameters. It generates all the combinations of the available factors,
 *  which take their values in predefined ranges.
 *
 * <h3>Example</h3>
 * <p>The following example shows how to explore in an exhaustive way the combination
 * between the parameters:
 * <pre>
 * p1, an integer taking its values in  [1,2]
 * p2, a big decimal taking its values in  [0.1,0.2]
 * p3, a double taking its 4 values in a logarithimic range [0.001,1]
 * </pre>
 *
 * <pre>{@code
 * import org.openmole.plugin.domain.interval.LogarithmRangeDouble
 * import org.openmole.plugin.domain.interval.RangeBigDecimal
 * import org.openmole.plugin.domain.interval.RangeInteger
 * import org.openmole.plugin.plan.complete.CompletePlan
 * import org.openmole.plugin.task.groovy.GroovyTask
 * import static org.openmole.ui.plugin.transitionfactory.TransitionFactory.*;
 *
 * p1 = builder.buildPrototype("p1", Integer.class)
 * p2 = builder.buildPrototype("p2", BigDecimal.class)
 * p3 = builder.buildPrototype("p3", Double.class)
 *
 * dataSet = builder.buildDataSet(p1,p2,p3)
 * plan = new CompletePlan(builder.exploration.buildFactor(p1, new RangeInteger("1","2","1")),
 *                         builder.exploration.buildFactor(p2, new RangeBigDecimal("0.1","0.2","0.1")),
 *                         builder.exploration.buildFactor(p3, new LogarithmRangeDouble("0.001","1","4")))
 *
 * explorationTask = builder.exploration.buildExplorationTask("explorationTask",plan)
 *
 * displayTask = new GroovyTask("displayTask")
 * displayTask.addInput(dataSet)
 * displayTask.setCode("println 'Run '+${p1}+', '+${p2}+', '+${p3}")
 *
 * exploration = buildExploration(explorationTask,
 *                                buildChain(displayTask))
 * builder.buildMole(exploration.getFirstCapsule()).run()}
 * </pre>
 *
 * gives the following output:
 * <pre>
 * Run 1, 0.1, 0.0010000000000000002
 * Run 2, 0.1, 0.0010000000000000002
 * Run 1, 0.2, 0.0010000000000000002
 * Run 2, 0.2, 0.0010000000000000002
 * Run 1, 0.1, 0.005623413251903492
 * Run 2, 0.1, 0.005623413251903492
 * Run 1, 0.2, 0.005623413251903492
 * Run 2, 0.2, 0.005623413251903492
 * Run 1, 0.1, 0.0316227766016838
 * Run 2, 0.1, 0.0316227766016838
 * Run 1, 0.2, 0.0316227766016838
 * Run 2, 0.2, 0.0316227766016838
 * Run 1, 0.1, 0.1778279410038923
 * Run 2, 0.1, 0.1778279410038923
 * Run 1, 0.2, 0.1778279410038923
 * Run 2, 0.2, 0.1778279410038923
 * Run 1, 0.1, 1.0
 * Run 2, 0.1, 1.0
 * Run 1, 0.2, 1.0
 * Run 2, 0.2, 1.0
 * </pre>
 */
package org.openmole.plugin.plan.complete;