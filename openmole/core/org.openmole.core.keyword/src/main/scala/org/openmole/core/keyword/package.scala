package org.openmole.core

/**
 * Generic keywords and their DSL
 */
package keyword:

  import monocle.*

  infix case class In[+A, +B](value: A, domain: B)
  infix case class Under[+A, +B](value: A, under: B)
  infix case class :=[+A, +B](value: A, equal: B)
  infix case class Negative[+A](value: A)

  // Covariance on type B causes problem for Some implicit conversion, see: DirectSampling
  infix case class Evaluate[+A, B](value: A, evaluate: B)

  infix case class Delta[+A, +B](value: A, delta: B)
  infix case class As[+A, +B](value: A, as: B)

  infix case class Weight[+A, +B](value: A, weight: B)

  infix case class Or[+A, +B](value: A, or: B)

  object By:
    def value[A, B] = Focus[By[A, B]](_.value)

  case class By[+A, +B](value: A, by: B)

  object On:
    def value[A, B] = Focus[On[A, B]](_.value)

  case class On[+A, +B](value: A, on: B)

  trait KeyWordPackage:
    implicit class InDecorator[A](a: A):
      infix def in[B](b: B): In[A, B] = In(a, b)

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
      //def aggregate[B, C](b: B => C) = Aggregate(a, b)

    implicit class DeltaDecorator[A](a: A):
      infix def delta[B](b: B) = Delta(a, b)

    implicit class AsDecorator[A](a: A):
      infix def as[B](b: B) = As(a, b)

    implicit class ByDecorator[A](a: A):
      infix def by[B](b: B) = By(a, b)

    implicit class OnDecorator[A](a: A):
      infix def on[B](b: B) = On(a, b)

    implicit class WeightDecorator[A](a: A):
      infix def weight[B](b: B) = Weight(a, b)

    implicit class OrDecorator[A](a: A):
      infix def or[B](b: B) = Or(a, b)

package object keyword extends KeyWordPackage
