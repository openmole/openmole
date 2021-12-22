package org.openmole.core.workflow

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import org.openmole.core.serializer.SerializerService

package object test {
  
  def serializeDeserialize[T](o: T) = {
    val builder = new ByteArrayOutputStream()
    serializer.serialize(o, builder)
    serializer.deserialize[T](new ByteArrayInputStream(builder.toByteArray))
  }

  export Stubs.*
}
