package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.tool.csv._

object SaltelliHook {

  def apply(method: Sensitivity.SaltelliMethodContainer, dir: FromContext[File])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("SaltelliHook") { p ⇒
      import p._

      val inputs = ScalarOrSequenceOfDouble.prototypes(method.inputs)

      val filePathFO = dir / "firstOrderIndices.csv"
      val fileFO = filePathFO.from(context)
      Sensitivity.writeFile(fileFO, inputs, method.outputs, Saltelli.firstOrder(_, _)).from(context)

      val filePathTO = dir / "firstOrderTotalOrder.csv"
      val fileTO = filePathTO.from(context)
      Sensitivity.writeFile(fileTO, inputs, method.outputs, Saltelli.totalOrder(_, _)).from(context)

      context
    }

}