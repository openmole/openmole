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
package renderer

/**
 * This renderer produces a plain text version of the BibTeX database.
 *
 * @author Lucas Satabin
 *
 */
class SimpleRenderer(db: BibTeXDatabase)
    extends BibTeXRenderer[String](db) with UniformRenderer[String] {

  protected[this] def render(groups: List[(String, List[BibEntry])]) = {
    groups.map {
      case (key, entries) ⇒
        "====== " + key + " ======\n\n" +
          entries.map {
            case BibEntry(name, key, fields) ⇒
              "==== " + name + " (" + key + ") ====\n" +
                fields.values.map {
                  case Field(fname, fvalue) ⇒
                    "  " + fname + ": " + fvalue
                }.mkString("\n")
          }.mkString("\n\n")
    }.mkString("\n\n")
  }

  protected[this] def renderAny(entry: BibEntry) = entry match {
    case BibEntry(name, key, fields) ⇒
      "==== " + name + " (" + key + ") ====\n" +
        fields.values.map {
          case Field(fname, fvalue) ⇒
            "  " + fname + ": " + fvalue
        }.mkString("\n")
  }

}