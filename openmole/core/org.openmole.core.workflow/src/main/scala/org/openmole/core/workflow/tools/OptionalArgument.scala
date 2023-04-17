package org.openmole.core.workflow.tools

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.{ ExpandedString, FromContext, ToFromContext }

object OptionalArgument:
  implicit def valueToOptionalOfFromContext[T1, T2](v: T1)(implicit toFromContext: ToFromContext[T1, T2]): OptionalArgument[FromContext[T2]] = OptionalArgument(FromContext.contextConverter(v))
  implicit def valueToOptionalArgument[T](v: T): OptionalArgument[T] = OptionalArgument(v)
  implicit def noneToOptionalArgument[T](n: None.type): OptionalArgument[T] = OptionalArgument.empty[T]

  given [T]: Conversion[OptionalArgument[T], Option[T]] = o => o

  def empty[T]: OptionalArgument[T] = None
  def apply[T](t: T): OptionalArgument[T] = Some(t)
  def apply[T](t: Option[T] = None): OptionalArgument[T] = t

  extension[T] (o: OptionalArgument[T])
    def option: Option[T] = o
    def toOption: Option[T] = option
    def mustBeDefined(name: String): T = option.getOrElse(throw new UserBadDataError(s"Parameter $name has not been set."))

opaque type OptionalArgument[+T] = Option[T]
