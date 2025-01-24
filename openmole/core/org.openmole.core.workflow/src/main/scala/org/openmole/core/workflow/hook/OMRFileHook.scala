package org.openmole.core.workflow.hook

import org.openmole.core.context.*
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.argument.FromContext
import org.openmole.core.setter.*
import org.openmole.core.workflow.dsl.*
import org.openmole.core.format.OutputFormat.*
import org.openmole.core.format.*
import org.openmole.core.script.ScriptSourceData

object OMRFileHook:

  def apply[M](
    output:   WritableOutput,
    metadata: M,
    values:   Seq[Val[?]]    = Vector.empty,
    exclude:  Seq[Val[?]]    = Vector.empty,
    option: OMROption = OMROption(),
    name:     Option[String] = None)(implicit valName: sourcecode.Name, definitionScope: DefinitionScope, methodData: MethodMetaData[M], scriptData: ScriptSourceData): FromContextHook =

    Hook(name getOrElse "OMRFileHook"): parameters =>
      import parameters._

      val excludeSet = exclude.map(_.name).toSet
      val ps = { if (values.isEmpty) context.variables.values.map { _.prototype }.toVector else values }.filter { v => !excludeSet.contains(v.name) }

      val variables = ps.map(p => context.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p not found in hook $this")))
      val content = OutputContent(variables)

      OMROutputFormat.write(executionContext, output, content, metadata, option).from(context)
      context
    .withValidate { WritableOutput.file(output).toSeq.flatMap(_.validate) } set (inputs ++= values)


