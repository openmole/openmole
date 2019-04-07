package org.openmole.plugin.method.evolution

import shapeless._
import ops._
import ops.hlist._

trait WorkflowIntegrationSelector[L <: HList, U] extends DepFn1[L] with Serializable {
  type Out = U
  def selected: WorkflowIntegration[U]
}

object WorkflowIntegrationSelector {

  def apply[L <: HList, U](implicit selector: WorkflowIntegrationSelector[L, U]): WorkflowIntegrationSelector[L, U] = selector

  implicit def select[H, T <: HList](implicit wii: WorkflowIntegration[H]): WorkflowIntegrationSelector[H :: T, H] =
    new WorkflowIntegrationSelector[H :: T, H] {
      def apply(l: H :: T) = l.head
      def selected = wii
    }

  implicit def recurse[H, T <: HList, U](implicit st: WorkflowIntegrationSelector[T, U], wii: WorkflowIntegration[U]): WorkflowIntegrationSelector[H :: T, U] =
    new WorkflowIntegrationSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.tail)
      def selected = wii
    }
}

