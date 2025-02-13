package org.openmole.core.argument

import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.types.{ToDouble, TypeConverter}

trait LowPriorityOptionalArgumentImplicit:
  given [T]: Conversion[T, OptionalArgument[T]] = v => OptionalArgument(v)

object OptionalArgument extends LowPriorityOptionalArgumentImplicit:
  given [T]: Conversion[None.type, OptionalArgument[Nothing]] = _ => OptionalArgument.empty
  given [T]: Conversion[OptionalArgument[T], Option[T]] = o => o
  given [T1, T2](using toFromContext: ToFromContext[T1, T2]): Conversion[T1, OptionalArgument[FromContext[T2]]] = v => OptionalArgument(FromContext.contextConverter(v))
  given [T1, T2](using tc: TypeConverter[T1, T2]): Conversion[T1, OptionalArgument[T2]] = v => OptionalArgument(summon[TypeConverter[T1, T2]].apply(v))

  def empty: OptionalArgument[Nothing] = None
  def apply[T](t: T): OptionalArgument[T] = Some(t)
  def apply[T](t: Option[T] = None): OptionalArgument[T] = t

  extension[T] (o: OptionalArgument[T])
    def option: Option[T] = o
    def toOption: Option[T] = option
    def mustBeDefined(name: String): T = option.getOrElse(throw new UserBadDataError(s"Parameter $name has not been set."))

opaque type OptionalArgument[+T] = Option[T]
