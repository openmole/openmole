/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.task

import org.openmole.core.context.Val
import org.openmole.core.workflow.dsl._

package netlogo {

  import org.openmole.core.workflow.builder._

  trait NetLogoPackage extends external.ExternalPackage {
    lazy val netLogoInputs = new {
      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](p: Val[_], n: String): T ⇒ T =
        implicitly[NetLogoTaskBuilder[T]].netLogoInputs.modify(_ ++ Seq(p → n)) andThen (inputs += p)
      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](p: Val[_]): T ⇒ T = this.+=[T](p, p.name)
    }

    lazy val netLogoArrayInputs = new {
      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](p: Val[_], n: String): T ⇒ T =
        implicitly[NetLogoTaskBuilder[T]].netLogoArrayInputs.modify(_ ++ Seq(p → n)) andThen (inputs += p)
      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](p: Val[_]): T ⇒ T = this.+=[T](p, p.name)
    }

    lazy val netLogoOutputs = new {
      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](name: String, column: Int, p: Val[_]): T ⇒ T =
        implicitly[NetLogoTaskBuilder[T]].netLogoArrayOutputs.modify(_ ++ Seq((name, column, p))) andThen (outputs += p)

      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](n: String, p: Val[_]): T ⇒ T =
        implicitly[NetLogoTaskBuilder[T]].netLogoOutputs.modify(_ ++ Seq(n → p)) andThen (outputs += p)

      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](p: Val[_]): T ⇒ T = this.+=[T](p.name, p)
    }

    lazy val netLogoArrayOutputs = new {
      def +=[T: NetLogoTaskBuilder: InputOutputBuilder](name: String, column: Int, p: Val[_]): T ⇒ T =
        implicitly[NetLogoTaskBuilder[T]].netLogoArrayOutputs.modify(_ ++ Seq((name, column, p))) andThen (outputs += p)
    }

  }
}

package object netlogo extends NetLogoPackage