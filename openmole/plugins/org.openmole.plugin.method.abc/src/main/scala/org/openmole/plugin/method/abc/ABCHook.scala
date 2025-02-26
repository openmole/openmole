package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object ABCHook {

  def apply(method: ABC.ABCParameters, dir: FromContext[File], frequency: Long = 1)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Hook("ABCHook") { p =>
      import p._

      if (context(method.step) % frequency == 0) {
        context(method.state) match {
          case MonAPMC.Empty() => ()
          case MonAPMC.State(_, s) =>
            val step = context(method.step)

            val filePath = dir / s"step${step}.csv"
            val file = filePath.from(context)

            val size = s.thetas.size
            val dim = s.thetas(0).size

            val paramNames = method.prior.v.map { _.name }

            val header =
              (Vector("epsilon,pAcc,t,ts,rhos,weight") ++
                paramNames).mkString(",")

            val data =
              (Vector.fill(size)(s.epsilon) zip
                Vector.fill(size)(s.pAcc) zip
                Vector.fill(size)(s.t) zip
                s.ts zip
                s.rhos zip
                s.weights zip
                s.thetas).map {
                  case ((((((epsilon, pAcc), t), ti), rhoi), wi), thetai) =>
                    epsilon.formatted("%.12f") ++ "," ++
                      pAcc.formatted("%.12f") ++ "," ++
                      t.formatted("%d") ++ "," ++
                      ti.formatted("%d") ++ "," ++
                      rhoi.formatted("%.12f") ++ "," ++
                      wi.formatted("%.12f") ++ "," ++
                      thetai.map { _.formatted("%.12f") }.mkString(",")
                }.mkString("\n")

            file.createParentDirectory

            file.content = header ++ "\n" ++ data

        }
      }

      context
    }

}
