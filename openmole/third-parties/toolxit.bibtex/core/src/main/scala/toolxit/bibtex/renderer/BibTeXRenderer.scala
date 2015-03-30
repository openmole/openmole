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

import scala.collection.mutable.{ HashMap, MultiMap, LinkedHashSet }

/**
 * A BibTeX renderer allows the user to output a BibTeX database in
 * a special format.
 *
 * @author Lucas Satabin
 *
 */
abstract class BibTeXRenderer[Rendered](val db: BibTeXDatabase)(implicit val defaultStrings: Map[String, String]) {

  private[this] var _groupByField: Option[String] = None
  private[this] var _groupDirection: Direction = Ascending
  private[this] var _groupByType = false
  private[this] var _filter: Option[Filter] = None
  private[this] var _sortBy: Option[String] = None

  private[this] var _cached: Option[Rendered] = None

  /**
   * The formatter used to format the author names.
   * By default the format is First von Last, jr
   * Override this to change name formatting.
   */
  protected[this] val nameFormatter = new NameFormatter("{ff}{{ }vv}{{ }ll}{{, }jj}")

  /**
   * The separator put between each author name.
   * By default, this is a comma.
   * Override this to change name separator.
   */
  protected[this] val nameSeparator = ", "

  /**
   * The formatters that may be applied to strings before rendering them
   * (in particular for author names).
   * The order in the list matters as they will be applied in this order
   * and result may be different. To chain several formatters, use the
   * `andThen' operator:
   * <pre>
   * override protected[this] lazy val stringFormatter = formatter1 andThen formatter2
   * </pre>
   * By default, nothing is done.
   * Override this to change string formatting.
   */
  protected[this] lazy val stringFormatter: StringFormatter = new StringFormatter {
    def apply(sentence: List[Word]) = sentence // do nothing by default
  }

  /**
   * Takes a string that is a list of names and returns the formatted
   * version of this string according to formatter given in nameFormatters
   */
  def formatNames(names: String) =
    AuthorNamesExtractor.toList(names).map(
      nameFormatter andThen stringFormatter).mkString(nameSeparator)

  /**
   * Takes a BibTeX database and returns its rendered string representation.
   * The result is cached for more efficiency if it is called again.
   */
  def render: Rendered = _cached match {
    case Some(cached) ⇒
      cached
    case _ ⇒
      val res = render(groups)
      _cached = Some(res)
      res
  }

  /**
   * Renders the entries filter, sorted and grouped.
   * Implementors must only implement this method
   */
  protected[this] def render(groups: List[(String, List[BibEntry])]): Rendered

  /** Renders the entry identified by the key. If entry is not found, returns None */
  def render(key: String): Option[Rendered] = db.find(key).map(render _)

  /** Renders the given single entry */
  protected[this] def render(entry: BibEntry): Rendered = entry.name match {
    case "article"       ⇒ renderArticle(entry)
    case "book"          ⇒ renderBook(entry)
    case "booklet"       ⇒ renderBooklet(entry)
    case "conference"    ⇒ renderConference(entry)
    case "inbook"        ⇒ renderInBook(entry)
    case "incollection"  ⇒ renderInCollection(entry)
    case "inproceedings" ⇒ renderInProceedings(entry)
    case "manual"        ⇒ renderManual(entry)
    case "masterthesis"  ⇒ renderMasterThesis(entry)
    case "misc"          ⇒ renderMisc(entry)
    case "phdthesis"     ⇒ renderPhdThesis(entry)
    case "proceedings"   ⇒ renderProceedings(entry)
    case "techreport"    ⇒ renderTechReport(entry)
    case "unpublished"   ⇒ renderUnpublished(entry)
    case _               ⇒ renderUnknown(entry)
  }

  /** Renders an article */
  def renderArticle(entry: BibEntry): Rendered

  /** Renders a book */
  def renderBook(entry: BibEntry): Rendered

  /** Renders a booklet */
  def renderBooklet(entry: BibEntry): Rendered

  /** Renders a conference */
  def renderConference(entry: BibEntry): Rendered

  /** Renders an inbook */
  def renderInBook(entry: BibEntry): Rendered

  /** Renders an incollection */
  def renderInCollection(entry: BibEntry): Rendered

  /** Renders an inproceedings */
  def renderInProceedings(entry: BibEntry): Rendered

  /** Renders a manual */
  def renderManual(entry: BibEntry): Rendered

  /** Renders a masterthesis */
  def renderMasterThesis(entry: BibEntry): Rendered

  /** Renders a misc */
  def renderMisc(entry: BibEntry): Rendered

  /** Renders a phdthesis */
  def renderPhdThesis(entry: BibEntry): Rendered

  /** Renders a proceedings */
  def renderProceedings(entry: BibEntry): Rendered

  /** Renders a techreport */
  def renderTechReport(entry: BibEntry): Rendered

  /** Renders a unpublished */
  def renderUnpublished(entry: BibEntry): Rendered

  /** Renders an unknown */
  def renderUnknown(entry: BibEntry): Rendered

  /** Clears the cached value */
  def clearCache = _cached = None

  /**
   * The renderer will group entries by the given field.
   * If the field does not exist for an entry, the renderer implementation may decide
   * under which category to group this entry.
   * This method returns this renderer object to allow the user to chain calls.
   */
  def groupByField(fieldName: String,
                   direction: Direction = Ascending): this.type = modify {
    _groupByField = Option(fieldName)
    _groupDirection = direction
  }

  /**
   * The renderer will group entries by entry type.
   * This method returns this renderer object to allow the user to chain calls.
   */
  def groupByType(direction: Direction = Ascending): this.type = modify {
    _groupByType = true
    _groupDirection = direction
  }

  /**
   * The renderer will only render entries matching the given filter.
   * This method returns this renderer object to allow the user to chain calls.
   */
  def filter(filter: Filter): this.type = modify {
    _filter = Option(filter)
  }

  /**
   * The renderer will sort entries by the given field.
   * If the field does not exist for an entry, the entry comes after all other. These
   * entries are sorted by key.
   * This method returns this renderer object to allow the user to chain calls.
   */
  def sortBy(fieldName: String): this.type = modify {
    _sortBy = Option(fieldName)
  }

  // ==== helper methods ====

  /* buld the group list, filtered and sorted */
  private[this] def groups: List[(String, List[BibEntry])] = {

    val groups =
      new HashMap[String, LinkedHashSet[BibEntry]] {
        def addBinding(key: String, value: BibEntry): this.type = {
          get(key) match {
            case None ⇒
              val set = new LinkedHashSet[BibEntry]
              set += value
              this(key) = set
            case Some(set) ⇒
              set += value
          }
          this
        }
      }
    var env = defaultStrings //.toMap

    val filter = _filter.getOrElse(TrueFilter)

    // enrich environment with user defined strings
    db.strings.foreach {
      case StringEntry(name, value) ⇒
        env += (name -> value.resolve(env))
    }

    // create the groups of entries
    (_groupByField, _groupByType) match {
      case (Some(name), false) ⇒
        db.entries.foreach {
          case entry: BibEntry if filter.matches_?(entry) ⇒
            // if the entry matches the filter, add it
            val group = entry.field(name).getOrElse(EmptyValue)
            groups.addBinding(group.resolve(env), entry)
          case _ ⇒ // do nothing
        }
      case (None, true) ⇒
        db.entries.foreach {
          case entry: BibEntry if filter.matches_?(entry) ⇒
            // if the entry matches the filter, add it
            groups.addBinding(entry.name, entry)
          case _ ⇒ // do nothing
        }
      case _ ⇒
        db.entries.foreach {
          case entry: BibEntry if filter.matches_?(entry) ⇒
            // if the entry matches the filter, add it
            groups.addBinding("Entries", entry)
          case _ ⇒ // do nothing
        }
    }

    // sort elements for each group if sort field is defined
    val result = scala.collection.mutable.Map.empty[String, List[BibEntry]]
    _sortBy match {
      case Some(field) ⇒
        groups.keys.foreach { key ⇒
          result(key) = groups(key).toList.sortBy(_.field(field).getOrElse(EmptyValue))
        }
      case None ⇒
        groups.keys.foreach { key ⇒
          result(key) = groups(key).toList
        }
    }

    result.toList.sortWith { (first, second) ⇒
      _groupDirection match {
        case Ascending ⇒
          first._1 <= second._1
        case Descending ⇒
          first._1 >= second._1
      }
    }
  }

  /* The given block modifies this renderer, thus, invalidating the cache */
  private[this] def modify(block: ⇒ Any): this.type = {
    try {
      block
    }
    finally {
      _cached = None
    }
    this
  }

}