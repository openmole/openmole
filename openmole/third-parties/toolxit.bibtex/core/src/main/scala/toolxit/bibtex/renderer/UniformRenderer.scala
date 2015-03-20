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
 * Mix-in this trait to have a BibTeXRenderer that renders all the entries in
 * the same way.
 *
 * @author Lucas Satabin
 *
 */
trait UniformRenderer[Rendered] {

  this: BibTeXRenderer[Rendered] ⇒

  /** Renders an article */
  override def renderArticle(entry: BibEntry) = renderAny(entry)

  /** Renders a book */
  override def renderBook(entry: BibEntry) = renderAny(entry)

  /** Renders a booklet */
  override def renderBooklet(entry: BibEntry) = renderAny(entry)

  /** Renders a conference */
  override def renderConference(entry: BibEntry) = renderAny(entry)

  /** Renders an inbook */
  override def renderInBook(entry: BibEntry) = renderAny(entry)

  /** Renders an incollection */
  override def renderInCollection(entry: BibEntry) = renderAny(entry)

  /** Renders an inproceedings */
  override def renderInProceedings(entry: BibEntry) = renderAny(entry)

  /** Renders a manual */
  override def renderManual(entry: BibEntry) = renderAny(entry)

  /** Renders a masterthesis */
  override def renderMasterThesis(entry: BibEntry) = renderAny(entry)

  /** Renders a misc */
  override def renderMisc(entry: BibEntry) = renderAny(entry)

  /** Renders a phdthesis */
  override def renderPhdThesis(entry: BibEntry) = renderAny(entry)

  /** Renders a proceedings */
  override def renderProceedings(entry: BibEntry) = renderAny(entry)

  /** Renders a techreport */
  override def renderTechReport(entry: BibEntry) = renderAny(entry)

  /** Renders a unpublished */
  override def renderUnpublished(entry: BibEntry) = renderAny(entry)

  /** Renders an unknown */
  override def renderUnknown(entry: BibEntry) = renderAny(entry)

  protected[this] def renderAny(entry: BibEntry): Rendered

}