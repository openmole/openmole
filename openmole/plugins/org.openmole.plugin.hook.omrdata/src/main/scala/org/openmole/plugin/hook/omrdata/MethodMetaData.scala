package org.openmole.plugin.hook.omrdata

import io.circe.*

case class MethodMetaData[T](name: T => String)(using val encoder: Encoder[T])

