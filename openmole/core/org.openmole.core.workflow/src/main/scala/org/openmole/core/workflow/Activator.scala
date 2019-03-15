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
      import org.openmole.core.workflow.transition.{ TransitionSlot }
      import org.openmole.core.workflow.execution.{ LocalEnvironment }
      import org.openmole.core.workflow.task.{ EmptyTask, ExplorationTask, ClosureTask, ToArrayTask, MoleTask, FromContextTask }

      Vector(
        Word(classOf[Val[_]]),
        Word("Capsule"),
        Word("Slot"),
        Word("Capsule"),
        Word("Strain"),
        Word("in"),
        Word("is"),
        Word("on"),
        Word("by"),
        Word("set"),
        Word("+="),
        Word(":="),
        Word("/"),
        Word("inputs"),
        Word("outputs"),
        Word("hook"),
        Word("workDirectory"),
        Word("plugins"),
        Word("pluginsOf"),
        Transition("--"),
        Transition("-<"),
        Transition(">-"),
        Transition("-<-"),
        Transition(">|"),
        Transition("oo"),
        Environment(classOf[LocalEnvironment]),
        Task(objectName(EmptyTask)),
        Task(objectName(ExplorationTask)),
        Task(objectName(ClosureTask)),
        Task(objectName(ToArrayTask)),
        Task(objectName(MoleTask)),
        Task(objectName(FromContextTask)),
        Hook(objectName(FromContextHook))
      )
    }

    ConfigurationInfo.register(this, ConfigurationInfo.list(org.openmole.core.workflow.execution.Environment) ++ ConfigurationInfo.list(org.openmole.core.workflow.execution.LocalEnvironment))
    PluginInfo.register(this, keyWords = keyWords)
  }

}
