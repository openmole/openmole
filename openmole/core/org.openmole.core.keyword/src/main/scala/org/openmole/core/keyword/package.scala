package org.openmole.core

/**
 * Generic keywords and their DSL
 */
package keyword {

  import monocle._

  case class In[+A, +B](value: A, domain: B)
  case class Under[+A, +B](value: A, under: B)
  case class :=[+A, +B](value: A, equal: B)
  case class Negative[+A](value: A)

  // Covariance on type B causes problem for Some implicit conversion, see: DirectSampling
  case class Evaluate[+A, B](value: A, evaluate: B)

  case class Delta[+A, +B](value: A, delta: B)
  case class As[+A, +B](value: A, as: B)

  object By:
    def value[A, B] = Focus[By[A, B]](_.value)

  case class By[+A, +B](value: A, by: B)

  object On:
    def value[A, B] = Focus[On[A, B]](_.value)

  case class On[+A, +B](value: A, on: B)

  trait KeyWordPackage:
    implicit class InDecorator[A](a: A):
      infix def in[B](b: B) = In(a, b)

    implicit class UnderDecorator[A](a: A):
      infix def under[B](b: B) = Under(a, b)

    implicit class EqualDecorator[A](a: A):
      infix def :=[B](b: B) = new :=(a, b)

    implicit class NegativeDecorator[A](a: A):
      def unary_- = Negative(a)

    implicit class EvaluateDecorator[A](a: A):
      @deprecated("15", "use evaluate")
      infix def aggregate[B](b: B): Evaluate[A, B] = evaluate(b)
      infix def evaluate[B](b: B) = Evaluate(a, b)
      //def aggregate[B, C](b: B ⇒ C) = Aggregate(a, b)

    implicit class DeltaDecorator[A](a: A):
      infix def delta[B](b: B) = Delta(a, b)

    implicit class AsDecorator[A](a: A):
      infix def as[B](b: B) = As(a, b)

    implicit class ByDecorator[A](a: A):
      infix def by[B](b: B) = By(a, b)

    implicit class OnDecorator[A](a: A):
      infix def on[B](b: B) = On(a, b)

}

package object keyword extends KeyWordPackage
