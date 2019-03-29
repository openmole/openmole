package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.expansion._
import org.openmole.core.dsl._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.mole.FromContextHook
import shapeless.HNil
import shapeless.ops.hlist.Selector

object ABCHook {

  def apply[T <: HNil](algorithm: T, dir: FromContext[File], frequency: OptionalArgument[Long] = None)(implicit parametersExtractor: Selector[T, ABCParameters], name: sourcecode.Name, definitionScope: DefinitionScope) = {

    val parameters = parametersExtractor(algorithm)

    FromContextHook("ABCHook") { p ⇒
      import p._
      import org.openmole.plugin.tool.csv._

      context(parameters.state) match {
        case MonAPMC.Empty() ⇒ ()

        case MonAPMC.State(t0, s) ⇒

          // TODO: il faut que step prenne la valeur de la variable "step" définie dans abc.
          //val step = context(parameters.step)
          val step = s.t

          val filePath = dir / ExpandedString("step${" + step + "}.csv")
          val file = filePath.from(context)

          val size = s.thetas.size
          val dim = s.thetas(0).size

          val header =
            (Vector("epsilon,pAcc,t,ts,rhos,weight") ++
              Vector.tabulate(dim) { i ⇒ "theta" ++ i.toString })
              .mkString(",")

          val data =
            (Vector.fill(size)(s.epsilon) zip
              Vector.fill(size)(s.pAcc) zip
              Vector.fill(size)(s.t) zip
              s.ts zip
              s.rhos zip
              s.weights zip
              s.thetas).map {
                case ((((((epsilon, pAcc), t), ti), rhoi), wi), thetai) ⇒
                  epsilon.toString ++ "," ++
                    pAcc.toString ++ "," ++
                    t.toString ++ "," ++
                    ti.toString ++ "," ++
                    rhoi.toString ++ "," ++
                    wi.toString ++ "," ++
                    thetai.mkString(",")
              }.mkString("\n")

          file.createParentDir

          file.content = header ++ "\n" ++ data

        /*writeVariablesToCSV(
            resultFileLocation.from(context),
            resultVariables(algorithm, context).map(_.prototype.array),
            resultVariables(algorithm, context).map(_.value),
            overwrite = true
          )*/

      }

      context
    }

  }

}
