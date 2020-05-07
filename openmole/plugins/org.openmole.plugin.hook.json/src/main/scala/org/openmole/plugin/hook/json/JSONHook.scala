package org.openmole.plugin.hook.json

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.format.WritableOutput

object JSONHook {
  def apply(output: WritableOutput, values: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    apply(output, values.toVector)

  def apply(
    output:  WritableOutput,
    values:  Seq[Val[_]]    = Vector.empty,
    exclude: Seq[Val[_]]    = Vector.empty)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    FormattedFileHook(
      format = JSONOutputFormat(),
      output = output,
      values = values,
      exclude = exclude,
      name = Some("JSONHook")
    )
}
