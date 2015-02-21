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
package bst

import java.io.Writer
import freemarker.template._

import scala.collection.mutable.Map

/**
 * This class allows the user to generate a scala renderer class from
 * an input .bst file. The generated code is similar to what a user would
 * write using the BibTeXRenderer API.
 * The code is generated as follows:
 *  - the generated renderer is a string renderer,
 *  - each known entry type function generates one of the needed render method,
 *  - others functions are generated as private methods in the class,
 *  - macro definitions are defined as values in a companion object,
 *  - dotted names are converted to camel case names: for example `chop.name' becomes chopName
 *  - calls to built-in functions are transformed to calls to methods in the
 *    `renderer.BuiltIn' trait, that is mixed-in with the generated class.
 *
 * @author Lucas Satabin
 *
 */
class Bst2Scala(val bst: BstFile,
                val className: String,
                val packageName: String,
                out: Writer) {

  // ======== the rendering methods to generated ========

  private[this] var renderArticle: List[String] = Nil
  private[this] var renderBook: List[String] = Nil
  private[this] var renderBooklet: List[String] = Nil
  private[this] var renderConference: List[String] = Nil
  private[this] var renderInBook: List[String] = Nil
  private[this] var renderInCollection: List[String] = Nil
  private[this] var renderInProceedings: List[String] = Nil
  private[this] var renderManual: List[String] = Nil
  private[this] var renderMasterThesis: List[String] = Nil
  private[this] var renderMisc: List[String] = Nil
  private[this] var renderPhdThesis: List[String] = Nil
  private[this] var renderProceedings: List[String] = Nil
  private[this] var renderTechReport: List[String] = Nil
  private[this] var renderUnpublished: List[String] = Nil
  private[this] var renderUnknown: List[String] = Nil

  // ======== the group rendering method to generate ========
  private[this] val render: List[String] = Nil

  // ======== the template to fill-in ========
  private[this] lazy val template = {
    val cfg = new Configuration
    cfg.setClassForTemplateLoading(this.getClass, "/")
    cfg.setObjectWrapper(new DefaultObjectWrapper)
    cfg.getTemplate("toolxit/bibtex/bst/BstRenderer.template")
  }

  def translate(bstFile: BstFile) = {

    val values = Map.empty[String, Any]

    bstFile.commands.foreach {
      case BstEntry(fields, integers, strings) ⇒
        values("fields") = fields
        values("ints") = integers
        values("strings") = strings
      case BstExecute(name) ⇒

    }
  }

}