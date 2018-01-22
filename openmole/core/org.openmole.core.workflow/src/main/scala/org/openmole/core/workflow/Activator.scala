package org.openmole.core.workflow

import org.openmole.core.pluginmanager.PluginInfo
import org.openmole.core.preference.ConfigurationInfo
import org.osgi.framework.{ BundleActivator, BundleContext }

class Activator extends BundleActivator {

  override def stop(context: BundleContext): Unit = {
    ConfigurationInfo.unregister(this)
    PluginInfo.unregister(this)
  }
  override def start(context: BundleContext): Unit = {

    val keyWords = {
      import org.openmole.core.pluginmanager.KeyWord._
      import org.openmole.core.context._
      import org.openmole.core.workflow.mole._
      import org.openmole.core.workflow.transition.{ Slot }
      import org.openmole.core.workflow.execution.{ LocalEnvironment }
      import org.openmole.core.workflow.puzzle.Puzzle
      import org.openmole.core.workflow.task.{ EmptyTask, ExplorationTask, ClosureTask, ToArrayTask, MoleTask, FromContextTask }

      Vector(
        Word(classOf[Val[_]]),
        Word(classOf[Capsule]),
        Word(classOf[MasterCapsule]),
        Word(classOf[Slot]),
        Word(classOf[Puzzle]),
        Word("in"),
        Word("is"),
        Transition("--"),
        Transition("-<"),
        Transition(">-"),
        Transition("-<-"),
        Transition(">|"),
        Transition("--="),
        Transition("oo"),
        Environment(classOf[LocalEnvironment]),
        Task(EmptyTask.getClass),
        Task(ExplorationTask.getClass),
        Task(ClosureTask.getClass),
        Task(ToArrayTask.getClass),
        Task(MoleTask.getClass),
        Task(FromContextTask.getClass),
        Hook(FromContextHook.getClass)
      )
    }

    ConfigurationInfo.register(this, ConfigurationInfo.list(org.openmole.core.workflow.execution.Environment) ++ ConfigurationInfo.list(org.openmole.core.workflow.execution.LocalEnvironment))
    PluginInfo.register(this, keyWords = keyWords)
  }

}
