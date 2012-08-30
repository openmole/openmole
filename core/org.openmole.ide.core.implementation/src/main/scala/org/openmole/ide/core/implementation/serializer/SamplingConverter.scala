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

import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.mapper.Mapper
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dataproxy.SamplingDataProxyUI
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.ide.core.implementation.registry.KeyGenerator
import scala.collection.mutable.HashSet

class SamplingConverter(mapper: Mapper,
                        provider: ReflectionProvider) extends ReflectionConverter(mapper, provider) {

  val added = new HashSet[Int]

  override def marshal(o: Object,
                       writer: HierarchicalStreamWriter,
                       mc: MarshallingContext) = {
    o match {
      case s: ISamplingDataProxyUI ⇒ added += s.id
      case _ ⇒
    }
    super.marshal(o, writer, mc)
  }

  override def unmarshal(reader: HierarchicalStreamReader,
                         uc: UnmarshallingContext) = {
    val samplingProxy = super.unmarshal(reader, uc)
    samplingProxy match {
      case s: ISamplingDataProxyUI ⇒ addSampling(s)
      case _ ⇒
    }
    samplingProxy
  }

  override def canConvert(t: Class[_]) = t.isAssignableFrom(classOf[SamplingDataProxyUI])

  def addSampling(s: ISamplingDataProxyUI) =
    if (!Proxys.samplings.map { ss ⇒ KeyGenerator(ss.getClass) }.contains(KeyGenerator(s.getClass))) {
      Proxys.samplings += s
      ConceptMenu.samplingMenu.popup.contents += ConceptMenu.addItem(s)
    }
}
