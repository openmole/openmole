package org.openmole.core.metadata

import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._

object MetadataRegistry {
  case class MethodSerializer(read: java.io.File => Any)
  val all = new ConcurrentHashMap[MethodName, MethodSerializer]().asScala
}
