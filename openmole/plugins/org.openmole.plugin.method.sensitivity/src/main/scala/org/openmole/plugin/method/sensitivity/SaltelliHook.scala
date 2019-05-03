package org.openmole.plugin.method.sensitivity

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SaltelliHook {

  def apply(dsl: DSLContainer[Sensitivity.SaltelliParams], dir: FromContext[File])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("SaltelliHook") { p ⇒
      import p._

      val inputs = ScalarOrSequenceOfDouble.prototypes(dsl.data.inputs)

      val filePathFO = dir / "firstOrderIndices.csv"
      val fileFO = filePathFO.from(context)
      Sensitivity.writeFile(fileFO, inputs, dsl.data.outputs, Saltelli.firstOrder(_, _)).from(context)

      val filePathTO = dir / "firstOrderTotalOrder.csv"
      val fileTO = filePathTO.from(context)
      Sensitivity.writeFile(fileTO, inputs, dsl.data.outputs, Saltelli.totalOrder(_, _)).from(context)

      context
    }

}