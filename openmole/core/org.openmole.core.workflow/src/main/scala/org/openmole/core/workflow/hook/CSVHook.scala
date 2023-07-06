package org.openmole.core.workflow.hook

import java.io.PrintStream

import org.openmole.core.context.{ Val, Variable }
import org.openmole.core.csv
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.format.{ CSVOutputFormat, WritableOutput }
import org.openmole.core.workflow.tools.OptionalArgument

object CSVHook {

  def apply(output: WritableOutput, values: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    apply(output, values.toVector)

  def apply(
    output:      WritableOutput,
    values:      Seq[Val[_]]                           = Vector.empty,
    exclude:     Seq[Val[_]]                           = Vector.empty,
    header:      OptionalArgument[FromContext[String]] = None,
    unrollArray: Boolean                               = false,
    arrayOnRow:  Boolean                               = false,
    overwrite:   Boolean                               = false,
    postfix:     OptionalArgument[FromContext[String]] = None,
    directory:   Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    FormattedFileHook(
      format = CSVOutputFormat(header = header, unrollArray = unrollArray, arrayOnRow = arrayOnRow, postfix = postfix, directory = directory),
      output = output,
      values = values,
      exclude = exclude,
      append = !overwrite,
      name = Some("CSVHook")
    )

}
