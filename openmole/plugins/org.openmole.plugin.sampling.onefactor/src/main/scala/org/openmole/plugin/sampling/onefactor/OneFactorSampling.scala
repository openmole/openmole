
package org.openmole.plugin.sampling.onefactor

import org.openmole.core.context.{ Context, PrototypeSet, Val, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.Finite
import org.openmole.core.workflow.sampling.{ ExplicitSampling, Factor, FactorSampling, Sampling }

import scala.collection.mutable.ArrayBuffer

case class NominalFactor[D, T](factor: Factor[D, T], nominalValue: T, values: Finite[D, T]) {

  def prototype //: Val[T]
  = factor.value

  //def toValuesSampling: Sampling = FactorSampling[D, T](factor)(values)
  // discrete.iterator(f.domain).map(_.map { v ⇒ List(Variable(f.value, v)) })
  //def toValuesSampling: Iterator[Iterable[Variable[_]]] = values.iterator(factor.domain).from(context).map(_.map{v => List(Variable(f.value, v))})
  //[T]: (Val[T], FromContext[Iterator[T]])
  //def sampling[T] = (prototype, values.iterator(factor.domain))
  /*def sampling[T](context: Context): Sampling = {
    val vals = values.computeValues(factor.domain).from(context)
    println("sampling for proto " + prototype.name + " = " + vals)
    ExplicitSampling(prototype, vals)
  }*/

  /*def sampling //: FromContext[Iterable[_]] =
  FromContext { p ⇒
    import p._
    values.computeValues(factor.domain).from(context)
  }*/

  //[T]: Iterable[Variable[T]]
  //def nominalVar[T] = Seq(Variable(prototype, nominalValue))
  def nominalVar: Sampling = ExplicitSampling(prototype, Seq(nominalValue))

}

/*
object NominalFactor {
  def apply[D, T](factor: Factor[D, T], nominalValue: T)(implicit domain: Finite[D, T]): NominalFactor[D, T] = NominalFactor(factor, nominalValue, domain)
}
*/

object OneFactorSampling {
  def apply(factors: NominalFactor[_, _]*) = new OneFactorSampling(factors: _*)
}

case class OneFactorSampling(factors: NominalFactor[_, _]*) extends Sampling {

  override def inputs: PrototypeSet = PrototypeSet.empty

  override def prototypes: Iterable[Val[_]] = factors.map { _.prototype }

  override def apply() = FromContext {
    p ⇒
      import p._
      if (factors.isEmpty) Iterator.empty
      else factors.toIterator.flatMap { n: NominalFactor[_, _] ⇒ oneFactorSampling(n).from(context) }
  }

  /**
   * Sampling along one of parameters
   *   (the FromContext is needed as NominalFactor function return Sampling, since they use core sampling primitive)
   * @param n
   * @return
   */
  def oneFactorSampling[D, T](n: NominalFactor[D, T]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext {
    p ⇒
      import p._
      //val fullsampling: Sampling = ExplicitSampling(n.prototype, n.sampling.from(context))
      val fullsampling: Sampling = ExplicitSampling(n.prototype, n.values.computeValues(n.factor.domain).from(context))

      /*val fullsampling //: Seq[Iterator[Iterable[Variable[_]]]]
      = {
        //val sampling: (Val[_], FromContext[Iterator[_]]) = n.sampling
        val sampling = n.sampling
        //val proto: Val[T] = sampling._1
        //val fullvalues = for { v ← sampling._2.from(context) } yield v
        val fullvalues = new ArrayBuffer[T]
        sampling._2.from(context).foreach(v ⇒ fullvalues.append(v))
        println("full values = " + fullvalues.toSeq)
        Seq(fullvalues.map { v ⇒ List(Variable(proto, v)) }.toIterator)
      }*/
      val nomsamplings: Seq[Sampling] = factors.filter(!_.equals(n)).map { n ⇒ n.nominalVar }.toSeq
      //println("One factor for " + n + " : full = " + fullsampling + " ; nomsampling = " + nomsampling)
      val res = (Seq(fullsampling) ++ nomsamplings)
      //println("to reduce : " + res)
      complete(res).from(context)
  }

  /**
   * complete sampling
   * @param samplings
   * @return
   */
  def complete(samplings: Seq[Sampling]): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    //samplings.reduceLeft(combine)
    /*samplings.tail.foldLeft(samplings.head) {
      (s1, s2) ⇒
        {
          combine(s1, s2)
        }
    }*/
    samplings.tail.foldLeft(samplings.head().from(context)) {
      (a, b) ⇒ combine(a, b).from(context)
    }
  }

  /**
   * combination for the complete sampling
   * @param s1
   * @param s2
   * @return
   */
  def combine(s1: Iterator[Iterable[Variable[_]]], s2: Sampling): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    for (x ← s1; y ← s2().from(context ++ x)) yield x ++ y
  }

}
