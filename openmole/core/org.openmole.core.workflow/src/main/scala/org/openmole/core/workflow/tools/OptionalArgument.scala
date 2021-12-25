package org.openmole.core.workflow.tools

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.{ ExpandedString, FromContext, ToFromContext }

object OptionalArgument {
  implicit def valueToOptionalOfFromContext[T1, T2](v: T1)(implicit toFromContext: ToFromContext[T1, T2]): OptionalArgument[FromContext[T2]] = OptionalArgument(Some(FromContext.contextConverter(v)))
  implicit def valueToOptionalArgument[T](v: T): OptionalArgument[T] = OptionalArgument(Some(v))
  implicit def noneToOptionalArgument[T](n: None.type): OptionalArgument[T] = OptionalArgument.empty[T]

  def empty[T]: OptionalArgument[T] = OptionalArgument()
  def apply[T](t: T): OptionalArgument[T] = OptionalArgument(Some(t))
}

case class OptionalArgument[T](option: Option[T] = None) {
  def mustBeDefined(name: String) = option.getOrElse(throw new UserBadDataError(s"Parameter $name has not been set."))
  def toOption = option
}
