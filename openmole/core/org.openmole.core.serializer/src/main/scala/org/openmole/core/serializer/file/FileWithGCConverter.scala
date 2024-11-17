package org.openmole.core.serializer.file

import com.thoughtworks.xstream.converters.{ Converter, MarshallingContext, UnmarshallingContext }
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import org.openmole.core.fileservice.FileService.FileWithGC
import java.io.File

class FileWithGCConverter() extends Converter {
  override def marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext): Unit = {
    val file = new File(source.asInstanceOf[FileWithGC].getPath)
    context.convertAnother(file)
  }

  override def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): AnyRef = {
    context.convertAnother(context.currentObject(), classOf[File])
  }

  override def canConvert(`type`: Class[?]): Boolean = classOf[FileWithGC].isAssignableFrom(`type`)
}