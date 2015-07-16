package org.openmole.gui.server.core

import com.github.rjeschke._
import com.github.rjeschke.txtmark._

/*
 * Copyright (C) 16/07/15 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object MarkDownProcessor {

  class MyDecorator extends DefaultDecorator {
    override def openLink(out: java.lang.StringBuilder) = out.append("<a target=\"_blank\"")
  }

  def apply(mdContent: String): String = {
    val conf = Configuration.builder
    conf.setDecorator(new MyDecorator)
    txtmark.Processor.process(
      mdContent,
      conf.build
    )
  }

}