/*
 * Copyright (C) 2013 Mathieu Leclaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.domain.modifier

import org.openmole.ide.core.implementation.serializer.GUISerializer
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ModifierDomainUISpec extends FlatSpec with ShouldMatchers {

  "GroupDomainDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("group09.xml")) should equal(true)
  }

  "MapDomainDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("map09.xml")) should equal(true)
  }

  "SlidingDomainDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("sliding09.xml")) should equal(true)
  }

  "SortDomainDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("sort09.xml")) should equal(true)
  }

  "SortByNameDomainDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("sortbyname09.xml")) should equal(true)
  }

  "TakeDomainDataUI" should "be unserializable" in {
    GUISerializer.serializable(getClass.getClassLoader.getResource("take09.xml")) should equal(true)
  }
}
