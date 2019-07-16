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
  case class Delta[A, B](value: A, delta: B)

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
      def aggregate[B, C](b: B ⇒ C) = Aggregate(a, b)
    }

    implicit class DeltaDecorator[A](a: A) {
      def delta[B](b: B) = Delta(a, b)
    }
  }

}

package object keyword extends KeyWordPackage
