package org.openmole.tool.stream

import java.io.OutputStream
import reflect.Selectable.reflectiveSelectable

object MultiplexedOutputStream {
  type M = {
    def write(i: Int): Unit
    def flush(): Unit
  }

  def apply(streams: M*) =
    new MultiplexedOutputStream(streams)
}

class MultiplexedOutputStream(streams: Seq[MultiplexedOutputStream.M]) extends OutputStream {
  override def write(i: Int): Unit = streams.foreach(_.write(i))
  override def flush(): Unit = streams.foreach(_.flush())
}
