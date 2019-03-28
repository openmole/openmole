package org.openmole.plugin.method.abc

import org.openmole.core.dsl._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.mole.FromContextHook
import shapeless.HNil
import shapeless.ops.hlist.Selector

object ABCHook {

  def apply[T <: HNil](algorithm: T)(implicit parametersExtractor: Selector[T, ABCParameters], name: sourcecode.Name, definitionScope: DefinitionScope) = {

    val parameters = parametersExtractor(algorithm)

    FromContextHook("SaveLastPopulationHook") { p ⇒
      import p._
      import org.openmole.plugin.tool.csv._
      parameters.state
      context
    }

  }

}
