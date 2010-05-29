import org.openmole.core.workflow.implementation.data.*
import org.openmole.core.workflow.implementation.transition.*
import org.openmole.core.workflow.implementation.mole.Mole
import org.openmole.core.workflow.implementation.task.*
import org.openmole.core.workflow.implementation.capsule.*
import org.openmole.core.workflow.implementation.plan.*
import org.openmole.core.workflow.implementation.domain.*
import org.openmole.core.workflow.implementation.resource.*
import org.openmole.core.workflow.implementation.mole.execution.*
import org.simexplorer.openmole.plugin.task.sensitivitytask.*
import org.openmole.plugin.task.groovytask.*

f1 = new Prototype("f1", Double)
f2 = new Prototype("f2", Double)
f3 = new Prototype("f3", Double)

samplingSize=1000

plan = new FastPlan(samplingSize)
plan.addFactor(new Factor(f1, new RFunctionDomain("qunif","-1","1")))
plan.addFactor(new Factor(f2, new RFunctionDomain("qunif","-1","1")))
plan.addFactor(new Factor(f3, new RFunctionDomain("qunif","-1","1")))
explorationTask = new ExplorationTask("exploration", plan)

modelTask = new GroovyTask("model")
modelTask.addInput(f1)
modelTask.addInput(f2)
modelTask.addInput(f3)
modelTask.setCode('A=7.0\nB=0.1\ny = Math.sin(f1) + A * Math.pow(Math.sin(f2), 2) + B * Math.pow(f3, 4) * Math.sin(f1)')

tellTask = new TellTask("tell")

modelTask.addOutput(tellTask.getModelOutputPrototype())

reportTask = new GroovyTask("report")
reportTask.addInput(tellTask.getAnalysisI1Prototype())
reportTask.addInput(tellTask.getAnalysisItPrototype())
reportTask.setCode('println "First order = ${ ' + tellTask.getAnalysisI1Prototype().getName() + '}"\nprintln "Total order = ${ ' + tellTask.getAnalysisItPrototype().getName() + '}"')

explorationCapsule = new ExplorationTaskCapsule(explorationTask)
modelCapsule = new TaskCapsule(modelTask)
tellCapsule = new TaskCapsule(tellTask)
reportCapsule = new TaskCapsule(reportTask)

new ExplorationTransition(explorationCapsule, modelCapsule)
new AggregationTransition(modelCapsule, tellCapsule)
new SingleTransition(tellCapsule, reportCapsule)
new Mole(explorationCapsule).start()


