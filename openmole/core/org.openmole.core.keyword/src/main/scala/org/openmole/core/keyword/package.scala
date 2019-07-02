package org.openmole.core

/**
 * Generic keywords and their DSL
 */
package keyword {

  import scala.reflect.ClassTag

  case class In[A, B](value: A, domain: B)
  case class Under[A, B](value: A, under: B)
  case class :=[A, B](value: A, equal: B)
  case class Negative[A](value: A)
  case class Aggregate[A, B](value: A, aggregate: B)

  trait KeyWordPackage {
    implicit class InDecorator[A](a: A) {
      def in[B](b: B) = In(a, b)
    }

    implicit class UnderDecorator[A](a: A) {
      def under[B](b: B) = Under(a, b)
    }

    implicit class EqualDecorator[A](a: A) {
      def :=[B](b: B) = new :=(a, b)
    }

    implicit class NegativeDecorator[A](a: A) {
      def unary_- = Negative(a)
    }

    implicit class AggregateDecorator[A](a: A) {
      def aggregate[B, C](b: Array[B] ⇒ C) = Aggregate(a, b)
    }

    implicit def aggregationFunctionConvertor[B, C](f: Vector[B] ⇒ C) = (v: Array[B]) ⇒ f(v.toVector)
  }

}

package object keyword extends KeyWordPackage
