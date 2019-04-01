package org.openmole.core

/**
 * Generic keywords and their DSL
 */
package keyword {
  case class In[A, B](value: A, domain: B)
  case class Under[A, B](value: A, under: B)
  case class :=[A, B](value: A, equal: B)

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
  }

}

package object keyword extends KeyWordPackage
