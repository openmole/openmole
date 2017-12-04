package org.openmole.tool.stream

import java.io.OutputStream

object MultiplexedOutputStream {
  def apply(streams: OutputStream*) =
    new MultiplexedOutputStream(streams)
}

class MultiplexedOutputStream(streams: Seq[OutputStream]) extends OutputStream {
  override def write(i: Int): Unit = streams.foreach(_.write(i))
  override def flush(): Unit = streams.foreach(_.flush())
}
