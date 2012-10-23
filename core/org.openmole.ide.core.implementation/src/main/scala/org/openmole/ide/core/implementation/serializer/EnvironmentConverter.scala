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
import org.openmole.ide.core.implementation.dataproxy.EnvironmentDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.registry._
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.implementation.registry.KeyGenerator
import scala.collection.mutable.HashSet

class EnvironmentConverter(mapper: Mapper,
                           provider: ReflectionProvider,
                           val serializer: GUISerializer,
                           var state: SerializerState) extends ReflectionConverter(mapper, provider) {

  val added = new HashSet[Int]

  override def marshal(o: Object,
                       writer: HierarchicalStreamWriter,
                       mc: MarshallingContext) = {
    val env = o.asInstanceOf[IEnvironmentDataProxyUI]
    state.content.get(env) match {
      case None ⇒
        state.content += env -> new Serializing(env.id)
        marshal(o, writer, mc)
      case Some(Serializing(id)) ⇒
        state.content(env) = new Serialized(id)
        super.marshal(env, writer, mc)
      case Some(Serialized(id)) ⇒
        writer.addAttribute("id", id.toString)
    }
  }

  override def unmarshal(reader: HierarchicalStreamReader,
                         uc: UnmarshallingContext) = {
    val environmentProxy = super.unmarshal(reader, uc)
    environmentProxy match {
      case e: IEnvironmentDataProxyUI ⇒ addEnvironment(e)
      case _ ⇒ environmentProxy
    }
  }

  override def canConvert(t: Class[_]) = t.isAssignableFrom(classOf[EnvironmentDataProxyUI])

  def addEnvironment(e: IEnvironmentDataProxyUI): IEnvironmentDataProxyUI = {
    val key = KeyGenerator(e.getClass)
    if (!KeyRegistry.environmentProxyKeyMap.contains(key)) {
      Proxys.environments += e
      ConceptMenu.environmentMenu.popup.contents += ConceptMenu.addItem(e)
    }
    KeyRegistry.environmentProxyKeyMap(key)
  }
}