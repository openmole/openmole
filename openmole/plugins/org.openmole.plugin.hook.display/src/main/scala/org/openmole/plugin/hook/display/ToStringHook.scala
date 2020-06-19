package org.openmole.plugin.hook.display

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object ToStringHook {
  def apply(prototypes: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = DisplayHook(prototypes: _*)
}
