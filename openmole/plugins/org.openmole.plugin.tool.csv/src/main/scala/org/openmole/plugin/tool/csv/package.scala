/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.tool

import java.io.File

import org.openmole.core.workflow.data.Prototype
import org.openmole.core.dsl._

package csv {
  trait CSVPackage {
    lazy val columns = new {
      def +=[T: CSVToVariablesBuilder](proto: Prototype[_]): T ⇒ T = this.+=(proto.name, proto)
      def +=[T: CSVToVariablesBuilder](name: String, proto: Val[_]): T ⇒ T =
        (implicitly[CSVToVariablesBuilder[T]].columns add (name → proto)) andThen
          (outputs += proto)
    }
    lazy val fileColumns = new {
      def +=[T: CSVToVariablesBuilder](dir: File, proto: Prototype[File]): T ⇒ T =
        this.+=(proto.name, dir, proto)

      def +=[T: CSVToVariablesBuilder](name: String, dir: File, proto: Prototype[File]): T ⇒ T =
        (implicitly[CSVToVariablesBuilder[T]].fileColumns add (name, dir, proto)) andThen
          (outputs += proto)
    }
    lazy val separator = new {
      def :=[T: CSVToVariablesBuilder](s: Option[Char]): T ⇒ T =
        implicitly[CSVToVariablesBuilder[T]].separator.set(s)

    }
  }
}

package object csv extends CSVPackage
