package org.openmole.plugin.method.abc

import mgo.abc.MonAPMC
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

object ABCHook {

  def apply(method: ABC.ABCParameters, file: WritableOutput, frequency: Option[Long] = None, keepHistory: Boolean = false)(using sourcecode.Name, DefinitionScope, ScriptSourceData) =
    Hook("ABCHook") { p =>
      import p.*
      
      val stepValue = context(method.step)

      if (stepValue % frequency.getOrElse(1L) == 0) {
        context(method.state) match {
          case MonAPMC.Empty() => ()
          case MonAPMC.State(_, s) =>

            val size = s.thetas.size
            val dim = s.thetas(0).size

            val paramNames = method.prior.v.map { _.name }

            val epsilon = Val[Double]("epsilon", ABC.abcNamespace)
            val pAcc = Val[Double]("pAcc", ABC.abcNamespace)
            val t = Val[Int]("t", ABC.abcNamespace)
            val step = Val[Int]("step", ABC.abcNamespace)
            val ts = Val[Array[Int]]("ts", ABC.abcNamespace)
            val rhos = Val[Array[Double]]("rhos", ABC.abcNamespace)
            val weight = Val[Array[Double]]("weight", ABC.abcNamespace)
            val thetas = Val[Array[Array[Double]]]("thetas", ABC.abcNamespace)

            val variables =
              Seq[Variable[?]](
                step -> stepValue,
                epsilon -> s.epsilon,
                pAcc -> s.pAcc,
                t -> s.t,
                ts -> s.ts.toArray,
                rhos -> s.rhos,
                weight -> s.weights,
                thetas -> s.thetas
              )


            val content =
              OutputContent(
                SectionContent(
                  Some("parameters"),
                  variables
                )
              )

            OMROutputFormat.write(executionContext, file, content, None, OMROption(replace = !keepHistory)).from(context)
        }
      }

      context
    }

}
