package org.openmole.core

package keyword {
  case class In[A, B](value: A, domain: B)
  case class Under[A, B](value: A, under: B)

  trait KeyWordPackage {
    implicit class InDecorator[A](a: A) {
      def in[B](b: B) = In(a, b)
    }

    implicit class UnderDecorator[A](a: A) {
      def under[B](b: B) = Under(a, b)
    }
  }

}

package object keyword extends KeyWordPackage
