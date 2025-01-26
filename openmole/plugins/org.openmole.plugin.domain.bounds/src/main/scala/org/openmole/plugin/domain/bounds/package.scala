package org.openmole.plugin.domain.bounds

import org.openmole.core.argument.*
import org.openmole.core.workflow.domain.*
import org.openmole.tool.types.*

import cats.implicits.*

implicit def tupleOfStringToBounds[T: {FromString, Manifest}]: BoundedFromContextDomain[(String, String), T] =
  BoundedFromContextDomain: domain =>
    val minCode = FromContext.codeToFromContext[T](domain._1)
    val maxCode = FromContext.codeToFromContext[T](domain._2)
    Domain((minCode, maxCode))


implicit def tupleIsBounds[T]: BoundedDomain[(T, T), T] = BoundedDomain(domain => Domain(domain))
implicit def iterableOfTuplesIsBounds[T: Manifest]: BoundedDomain[Iterable[(T, T)], Array[T]] = BoundedDomain(domain => Domain((domain.unzip._1.toArray, domain.unzip._2.toArray)))
implicit def arrayOfTuplesIsBounds[T: Manifest]: BoundedDomain[Array[(T, T)], Array[T]] = BoundedDomain(domain => Domain((domain.map(_._1), domain.map(_._2))))

implicit def iterableOfStringTuplesIsBounds[T: {FromString, Manifest}]: BoundedFromContextDomain[Iterable[(String, String)], Array[T]] =
  BoundedFromContextDomain: domain =>
    def min = domain.map(_._1).toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v): FromContext[T]).map(_.toArray)
    def max = domain.map(_._2).toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v): FromContext[T]).map(_.toArray)
    Domain((min, max))


implicit def arrayOfStringTuplesIsBounds[T: {FromString, Manifest}]: BoundedFromContextDomain[Array[(String, String)], Array[T]] =
  BoundedFromContextDomain: domain =>
    def min = domain.map(_._1).toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v): FromContext[T]).map(_.toArray)
    def max = domain.map(_._2).toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v): FromContext[T]).map(_.toArray)
    Domain((min, max))


