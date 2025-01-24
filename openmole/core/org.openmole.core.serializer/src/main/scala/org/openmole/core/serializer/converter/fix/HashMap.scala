//package org.openmole.core.serializer.converter.fix
//
//import com.thoughtworks.xstream.mapper.Mapper
//import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
//import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
//import com.thoughtworks.xstream.converters.{ UnmarshallingContext, MarshallingContext }
//import scala.collection.immutable.HashMap
//
////TODO: try to get rid of this fix after scala 2.13 migration
//class HashMapConverter(mapper: Mapper) extends AbstractCollectionConverter(mapper) {
//
//  def canConvert(clazz: Class[?]) =
//    classOf[scala.collection.immutable.HashMap[_, _]] == clazz || scala.collection.immutable.HashMap.empty.getClass == clazz
//
//  def marshal(value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) = {
//    val list = value.asInstanceOf[HashMap[_, _]]
//    for ((k, v) ← list) {
//      writer.startNode("entry")
//      writeItem(k, context, writer)
//      writeItem(v, context, writer)
//      writer.endNode()
//    }
//  }
//
//  def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = {
//    val list = new scala.collection.mutable.ListBuffer[(Any, Any)]()
//    while (reader.hasMoreChildren()) {
//      reader.moveDown()
//      val k = readItem(reader, context, list)
//      val v = readItem(reader, context, list)
//      list += k -> v
//      reader.moveUp()
//    }
//    scala.collection.immutable.HashMap(list.toSeq *)
//  }
//}
