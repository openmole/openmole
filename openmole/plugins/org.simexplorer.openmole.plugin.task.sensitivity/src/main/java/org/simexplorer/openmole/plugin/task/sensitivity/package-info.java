/*
 *  Copyright (C) 2010 Cemagref
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
 * <p>The purpose of this plugin is to give access to some sensitivy analysis methods
 * available in the R platform, such as the fast99 method.
 *
 * <h3>Builder</h3>
 * <p>This plugin provides the class
 * {@link org.simexplorer.openmole.plugin.task.sensitivity.Sensitivity} as a
 * builder that will help you to define your sensitivity analysis workflow.
 * 
 * <p>If you're using the plugin from the console, an instance of the class 
 * <code>Sensivitity</code> is available with the variable <code>sensitivity</code>.
 *
 * <h3>Prerequisites</h3>
 * <p>The plugin needs the R software. For now, you have to run R with the Rserve mode,
 * see online documentation. <!-- TODO wiki link -->
 * We except to embed a prebuilt R distribution using recent virtualization task
 * (see previous blog entry). <!-- TODO wiki link -->
 *
 * <h3>Examples</h3>
 * <p>Take a look at this sample script:
 * <pre>{@code
 * sensitivity.addFactor("f1", Double, new RFunctionDomain("qunif","-pi","pi"))
 * sensitivity.addFactor("f2", Double, new RFunctionDomain("qunif","-pi","pi"))
 * sensitivity.addFactor("f3", Double, new RFunctionDomain("qunif","-pi","pi"))
 * modelTask = new GroovyTask("model")
 * modelTask.setCode('sleep(2)\ny = Math.sin(f1) + 7.0 * Math.pow(Math.sin(f2), 2) + 0.1 * Math.pow(f3, 4) * Math.sin(f1)')
 * sensitivity.setModelTask(modelTask)
 * reportTask = new GroovyTask("report")
 * reportTask.setCode('println "First order = ${I1}"\nprintln "Total order = ${It}"')
 * sensitivity.setReportTask(reportTask)
 * execution = sensitivity.fast99(1000)
 * execution.start()}
 * </pre>
 * <p>This script produces such a workflow:<br/>
 * <img alt="fast workflow example" src="doc-files/wffast.png"/>
 */
package org.simexplorer.openmole.plugin.task.sensitivity;
