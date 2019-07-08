package org.openmole.plugin.hook

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

package object display {
  def ToStringHook(prototypes: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = DisplayHook(prototypes: _*)
}
