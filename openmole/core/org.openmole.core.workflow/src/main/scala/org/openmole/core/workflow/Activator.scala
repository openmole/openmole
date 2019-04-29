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
        WordKeyWord(classOf[Val[_]]),
        WordKeyWord("Capsule"),
        WordKeyWord("Slot"),
        WordKeyWord("Capsule"),
        WordKeyWord("Strain"),
        WordKeyWord("in"),
        WordKeyWord("is"),
        WordKeyWord("on"),
        WordKeyWord("by"),
        WordKeyWord("set"),
        WordKeyWord("+="),
        WordKeyWord(":="),
        WordKeyWord("/"),
        WordKeyWord("inputs"),
        WordKeyWord("outputs"),
        WordKeyWord("hook"),
        WordKeyWord("workDirectory"),
        WordKeyWord("plugins"),
        WordKeyWord("pluginsOf"),
        TransitionKeyWord("--"),
        TransitionKeyWord("-<"),
        TransitionKeyWord(">-"),
        TransitionKeyWord("-<-"),
        TransitionKeyWord(">|"),
        TransitionKeyWord("oo"),
        EnvironmentKeyWord(classOf[LocalEnvironment]),
        TaskKeyWord(objectName(EmptyTask)),
        TaskKeyWord(objectName(ExplorationTask)),
        TaskKeyWord(objectName(ClosureTask)),
        TaskKeyWord(objectName(ToArrayTask)),
        TaskKeyWord(objectName(MoleTask)),
        TaskKeyWord(objectName(FromContextTask)),
        HookKeyWord(objectName(FromContextHook))
      )
    }

    ConfigurationInfo.register(this, ConfigurationInfo.list(org.openmole.core.workflow.execution.Environment) ++ ConfigurationInfo.list(org.openmole.core.workflow.execution.LocalEnvironment))
    PluginInfo.register(this, keyWords = keyWords)
  }

}
