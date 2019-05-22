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

import java.io.{ File, FileReader, PrintStream }
import java.math.{ BigDecimal, BigInteger }

import au.com.bytecode.opencsv.CSVReader
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.builder._

import scala.annotation.tailrec

package csv {

  trait CSVPackage {
    lazy val columns = new {
      def +=[T: CSVToVariablesBuilder: InputOutputBuilder](proto: Val[_]): T ⇒ T = this.+=(proto.name, proto)
      def +=[T: CSVToVariablesBuilder: InputOutputBuilder](name: String, proto: Val[_]): T ⇒ T =
        (implicitly[CSVToVariablesBuilder[T]].columns add Mapped(proto, name)) andThen (outputs += proto)
    }

    lazy val fileColumns = new {
      def +=[T: CSVToVariablesBuilder: InputOutputBuilder](dir: File, proto: Val[File]): T ⇒ T =
        this.+=(proto.name, dir, proto)

      def +=[T: CSVToVariablesBuilder: InputOutputBuilder](name: String, dir: File, proto: Val[File]): T ⇒ T =
        (implicitly[CSVToVariablesBuilder[T]].fileColumns add (name, dir, proto)) andThen (outputs += proto)
    }
    lazy val separator = new {
      def :=[T: CSVToVariablesBuilder](s: OptionalArgument[Char]): T ⇒ T =
        implicitly[CSVToVariablesBuilder[T]].separator.set(s)

    }
  }
}

package object csv extends CSVPackage {

}
