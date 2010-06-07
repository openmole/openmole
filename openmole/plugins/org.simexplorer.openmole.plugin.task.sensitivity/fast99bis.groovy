import org.openmole.plugin.task.groovy.GroovyTask

// optional: plugin reloading if needed
loaded=true;try { Class.forName("org.simexplorer.openmole.plugin.task.sensitivity.TellTask") } catch(Exception e) { loaded=false}
if (loaded) {
  plugin.unload('../../../../plugins/org.simexplorer.openmole.plugin.task.sensitivity/target/org.simexplorer.openmole.plugin.task.sensitivity-0.3.jar')
}
plugin.load('../../../../plugins/org.simexplorer.openmole.plugin.task.sensitivity/target/org.simexplorer.openmole.plugin.task.sensitivity-0.3.jar')

// regular import for the plugin
import org.simexplorer.openmole.plugin.task.sensitivity.*

// FIXME fails

sensitivity.addFactor("f1", Double, new RFunctionDomain("qunif","-pi","pi"))
sensitivity.addFactor("f2", Double, new RFunctionDomain("qunif","-pi","pi"))
sensitivity.addFactor("f3", Double, new RFunctionDomain("qunif","-pi","pi"))

modelTask = new GroovyTask("model")
modelTask.setCode('y = Math.sin(f1) + 7.0 * Math.pow(Math.sin(f2), 2) + 0.1 * Math.pow(f3, 4) * Math.sin(f1)')
sensitivity.setModelTask(modelTask)

reportTask = new GroovyTask("report")
reportTask.setCode('println "First order = ${ ' + TellTask.getAnalysisI1Prototype().getName() + '}"\nprintln "Total order = ${ ' + TellTask.getAnalysisItPrototype().getName() + '}"')
sensitivity.setReportTask(reportTask)

sensitivity.fast99(100)
