package org.openmole.core.workflow

import org.openmole.core.highlight.HighLight.*
import org.openmole.core.pluginregistry.PluginRegistry
import org.openmole.core.preference.PreferenceLocation
import org.openmole.core.argument.*
import org.openmole.core.workflow.composition.*
import org.openmole.core.workflow.task.*
import org.osgi.framework.{BundleActivator, BundleContext}

class Activator extends BundleActivator:

  override def stop(context: BundleContext): Unit = PluginRegistry.unregister(this)

  override def start(context: BundleContext): Unit = 

    val highLight = 
      import org.openmole.core.context._
      import org.openmole.core.workflow.execution.{ LocalEnvironment }
      import org.openmole.core.workflow.task.{ EmptyTask, ExplorationTask, ClosureTask, ToArrayTask, MoleTask, FromContextTask }

      Vector(
        WordHighLight(classOf[Val[?]]),
        WordHighLight(objectName(Capsule)),
        WordHighLight(objectName(Slot)),
        WordHighLight("Strain"),
        WordHighLight("in"),
        WordHighLight("is"),
        WordHighLight("on"),
        WordHighLight("by"),
        WordHighLight("set"),
        WordHighLight("+="),
        WordHighLight(":="),
        WordHighLight("/"),
        WordHighLight("inputs"),
        WordHighLight("outputs"),
        WordHighLight("hook"),
        WordHighLight("display"),
        WordHighLight("workDirectory"),
        WordHighLight("plugins"),
        WordHighLight("pluginsOf"),
        TransitionHighLight("--"),
        TransitionHighLight("-<"),
        TransitionHighLight(">-"),
        TransitionHighLight("-<-"),
        TransitionHighLight(">|"),
        TransitionHighLight("oo"),
        EnvironmentHighLight(classOf[LocalEnvironment]),
        TaskHighLight(objectName(EmptyTask)),
        TaskHighLight(objectName(ExplorationTask)),
        TaskHighLight(objectName(ClosureTask)),
        TaskHighLight(objectName(ToArrayTask)),
        TaskHighLight(objectName(MoleTask)),
        TaskHighLight(objectName(TryTask)),
        TaskHighLight(objectName(RetryTask)),
        OtherHighLight("OMR"),
        OtherHighLight("CSV")
      )

    PluginRegistry.register(this, highLight = highLight, preferenceLocation = PreferenceLocation.list(org.openmole.core.workflow.execution.Environment) ++ PreferenceLocation.list(org.openmole.core.workflow.execution.LocalEnvironment))


