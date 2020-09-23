package org.openmole.plugin.domain

import org.openmole.core.expansion._
import org.openmole.core.workflow.domain._
import org.openmole.tool.types._
import cats.implicits._

package object bounds {

  implicit def tupleOfStringToBounds[T: FromString: Manifest] = new BondsFromContext[(String, String), T] {
    override def min(domain: (String, String)): FromContext[T] = FromContext.codeToFromContext[T](domain._1)
    override def max(domain: (String, String)): FromContext[T] = FromContext.codeToFromContext[T](domain._2)
  }

  implicit def tupleIsBounds[T] = new Bounds[(T, T), T] {
    override def min(domain: (T, T)) = domain._1
    override def max(domain: (T, T)) = domain._2
  }

  implicit def iterableOfTuplesIsBounds[T: Manifest] = new Bounds[Iterable[(T, T)], Array[T]] {
    override def min(domain: Iterable[(T, T)]) = domain.unzip._1.toArray
    override def max(domain: Iterable[(T, T)]) = domain.unzip._2.toArray
  }

  implicit def arrayOfTuplesIsBounds[T: Manifest] = new Bounds[Array[(T, T)], Array[T]] {
    override def min(domain: Array[(T, T)]) = domain.unzip._1
    override def max(domain: Array[(T, T)]) = domain.unzip._2
  }

  implicit def iterableOfStringTuplesIsBounds[T: FromString: Manifest] = new BondsFromContext[Iterable[(String, String)], Array[T]] {
    override def min(domain: Iterable[(String, String)]) = domain.unzip._1.toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v)).map(_.toArray)
    override def max(domain: Iterable[(String, String)]) = domain.unzip._2.toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v)).map(_.toArray)
  }

  implicit def arrayOfStringTuplesIsBounds[T: FromString: Manifest] = new BondsFromContext[Array[(String, String)], Array[T]] {
    override def min(domain: Array[(String, String)]) = domain.unzip._1.toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v)).map(_.toArray)
    override def max(domain: Array[(String, String)]) = domain.unzip._2.toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v)).map(_.toArray)
  }

}
