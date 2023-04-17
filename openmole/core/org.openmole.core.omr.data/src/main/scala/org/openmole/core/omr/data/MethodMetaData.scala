package org.openmole.core.omr.data

import io.circe.*

case class MethodMetaData[T](plugin: T => String)(using val encoder: Encoder[T])

