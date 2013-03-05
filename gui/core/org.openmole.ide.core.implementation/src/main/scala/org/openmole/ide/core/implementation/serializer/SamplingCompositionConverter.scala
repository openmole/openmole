/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.serializer

import SerializerState._
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.mapper.Mapper
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.registry._
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.model.dataproxy.ISamplingCompositionDataProxyUI
import org.openmole.misc.exception.UserBadDataError

class SamplingCompositionConverter(mapper: Mapper,
                                   provider: ReflectionProvider,
                                   val serializer: GUISerializer,
                                   var state: SerializerState) extends ReflectionConverter(mapper, provider) {

  override def marshal(o: Object,
                       writer: HierarchicalStreamWriter,
                       mc: MarshallingContext) = {
    val sc = o.asInstanceOf[ISamplingCompositionDataProxyUI]
    state.content.get(sc) match {
      case None ⇒
        state.content += sc -> new Serializing(sc.id)
        marshal(o, writer, mc)
      case Some(Serializing(id)) ⇒
        state.content(sc) = new Serialized(id)
        super.marshal(sc, writer, mc)
      case Some(Serialized(id)) ⇒
        writer.addAttribute("id", id.toString)
    }
  }

  override def unmarshal(reader: HierarchicalStreamReader,
                         uc: UnmarshallingContext) = {
    if (reader.getAttributeCount != 0) {
      val existingSamplingComposition = Proxys.samplings.find(_.id == reader.getAttribute("id").toInt)
      existingSamplingComposition match {
        case Some(y: ISamplingCompositionDataProxyUI) ⇒ y
        case _ ⇒
          serializer.unserializeProxy("sampling")
          unmarshal(reader, uc)
      }
    } else {
      val o = super.unmarshal(reader, uc)
      o match {
        case y: ISamplingCompositionDataProxyUI ⇒
          if (Proxys.samplings.contains(y)) y
          else addSampling(y)
        case _ ⇒ throw new UserBadDataError("Can not load object " + o)
      }
    }
  }

  override def canConvert(t: Class[_]) = t.isAssignableFrom(classOf[SamplingCompositionDataProxyUI])

  def addSampling(s: ISamplingCompositionDataProxyUI): ISamplingCompositionDataProxyUI = {
    Proxys.samplings += s
    ConceptMenu.samplingMenu.popup.contents += ConceptMenu.addItem(s)
    s
  }
}