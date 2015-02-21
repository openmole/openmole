/*
* This file is part of the ToolXiT project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package toolxit.bibtex

import scala.util.parsing.input.Positional

abstract class BibtexField(val name: String, val value: Value) extends Positional {
  def toBibTeX = name + "=" + value.toBibTeX
}

case class Field(override val name: String, override val value: Value) extends BibtexField(name, value)

import util.Conversions._

// these case classes are very important since there is no type checking on the bibtex markups in toolxit-bibtex
case class Title(val inValue: String) extends BibtexField("Title", inValue)
case class Authors(val inValue: String*) extends BibtexField("Author", inValue.mkString(", "))
case class Journal(val inValue: String) extends BibtexField("Journal", inValue)
case class Year(val inValue: Int) extends BibtexField("Year", inValue)
case class Number(val inValue: Int) extends BibtexField("Number", inValue)
case class Pages(val inValue: String) extends BibtexField("Pages", inValue)
case class Volume(val inValue: Int) extends BibtexField("Volume", inValue)
case class Url(val inValue: String) extends BibtexField("Url", inValue)
