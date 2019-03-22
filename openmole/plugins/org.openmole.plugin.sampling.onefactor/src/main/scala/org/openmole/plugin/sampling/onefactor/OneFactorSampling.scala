
package org.openmole.plugin.sampling.onefactor

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.Finite
import org.openmole.core.workflow.sampling.{ ExplicitSampling, Factor, FactorSampling, Sampling }

case class NominalFactor[D, T](factor: Factor[D, T], nominalValue: T, values: Finite[D, T]) {

  def prototype: Val[T] = factor.value

  //def toValuesSampling: Sampling = FactorSampling[D, T](factor)(values)
  // discrete.iterator(f.domain).map(_.map { v ⇒ List(Variable(f.value, v)) })
  //def toValuesSampling: Iterator[Iterable[Variable[_]]] = values.iterator(factor.domain).from(context).map(_.map{v => List(Variable(f.value, v))})
  //[T]: (Val[T], FromContext[Iterator[T]])
  def sampling[T] = (prototype, values.iterator(factor.domain))

  //[T]: Iterable[Variable[T]]
  def nominalVar[T] = Seq(Variable(prototype, nominalValue))

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

  override def apply(): FromContext[Iterator[Iterable[Variable[_]]]] = FromContext { p ⇒
    import p._
    if (factors.isEmpty) Iterator.empty
    else
      factors.toIterator.flatMap(oneFactorSampling(_).from(context))
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
      val fullsampling //: Seq[Iterator[Iterable[Variable[_]]]]
      = {
        //val sampling: (Val[_], FromContext[Iterator[_]]) = n.sampling
        val sampling = n.sampling
        val proto: Val[T] = sampling._1
        val fullvalues = for { v ← sampling._2.from(context) } yield v
        println("full values = " + fullvalues.toSeq)
        Seq(fullvalues.map { v ⇒ List(Variable(proto, v)) })
      }
      val nomsampling //: Seq[Iterator[Iterable[Variable[_]]]]
      = factors.filter(!_.equals(n)).map { n ⇒ List(n.nominalVar).toIterator }
      println("One factor for " + n + " : full = " + fullsampling + " ; nomsampling = " + nomsampling)
      complete(fullsampling ++ nomsampling)
  }

  /**
   * complete sampling
   * @param samplings
   * @return
   */
  def complete(samplings: Seq[Iterator[Iterable[Variable[_]]]]): Iterator[Iterable[Variable[_]]] = samplings.reduceLeft(combine)

  /**
   * combination for the complete sampling
   * @param s1
   * @param s2
   * @return
   */
  def combine(s1: Iterator[Iterable[Variable[_]]], s2: Iterator[Iterable[Variable[_]]]): Iterator[Iterable[Variable[_]]] = for (x ← s1; y ← s2) yield {
    println(x)
    println(y)
    println("---------")
    x ++ y
  }

}
