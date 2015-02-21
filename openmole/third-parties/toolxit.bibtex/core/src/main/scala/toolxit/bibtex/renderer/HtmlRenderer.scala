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

import scala.xml._

/**
 * This renderer produces html.
 * This html is intended to be embedded in another html page, and thus the result
 * is not surrounded with the <pre><html></pre> and <pre><body></pre> tags.
 * This renderer also provides a way to retrieve a default CSS file that goes along
 * with the rendered html.
 * To customize the CSS, these are the selectors that exist:
 * <pre>
 * /* title */
 * .bibtitle { font-weight:bold; }
 * /* author */
 * .bibauthor { /* nothing by default */ }
 * /* booktitle (e.g. proceedings title, journal name, etc )*/
 * .bibbooktitle { font-style:italic; }
 * /* publisher */
 * .bibpublisher { /* nothing by default */ }
 * /* a group */
 * .group { /* nothing by default */ }
 *
 *
 * .title {
 * color: #003366;
 * font-size: large;
 * font-weight: bold;
 * text-align: right;
 * }
 *
 * .header {
 * background-color: #995124;
 * color: #FFFFFF;
 * padding: 1px 2px 1px 2px;
 * }
 *
 * .rheader {
 * font-weight: bold;
 * background-color: #003366;
 * color: #ffffff;
 * padding: 2px;
 * margin-bottom: 10px;
 * border-bottom: #ff6633 2px solid;
 *
 * }
 * .menu {
 * font-size: x-small;
 * background-color: #EFDDB4;
 * padding: 0px;
 * border: 1px solid #000000;
 * margin: 0px;
 * }
 * .menu a {
 * text-decoration: none;
 * color: #003366;
 * }
 * .menu a:hover {
 * color: #ff6633;
 * }
 *
 * .bibref {
 * padding:7px;
 * padding-left:15px;
 * vertical-align:text-top;
 * }
 *
 * .result {
 * padding:0px;
 * border: 1px solid #000000;
 * margin:0px;
 * background-color: #ffffff;
 * width:100%;
 * }
 * .result a {
 * text-decoration: none;
 * color: #469AF8;
 * }
 *
 * .result a:hover {
 * color: #ff6633;
 * }
 *
 * .input_box{
 * margin-bottom : 2px;
 * }
 * .mini_se {
 * border: none 0;
 * border-top: 1px dashed #717171;
 * height: 1px;
 * }
 * .a_name a {
 * color:#469AF8;
 * width:130px;
 * }
 *
 * .rsslink {
 * text-decoration: none;
 * color:#F88017;
 * /* could be fancy, see : http://www.feedicons.com/ for icons*/
 * /*background-image: url("rss.png"); text-indent: -9999px;*/
 * }
 *
 * .purebibtex {
 * font-family: monospace;
 * font-size: small;
 * border: 1px solid #DDDDDD;
 * white-space:pre;
 * background: none repeat scroll 0 0 #F5F5F5;
 * padding:10px;
 * }
 * </pre>
 *
 * @author Lucas Satabin
 *
 */
class HtmlRenderer(db: BibTeXDatabase)
    extends BibTeXRenderer[Node](db) {

  protected[this] def render(groups: List[(String, List[BibEntry])]) = {
    <table class="result">
      <tbody>
        {
          for ((group, entries) ← groups) yield {
            <tr class="group">
              <td class="header" colspan="2">{ group }</td>
            </tr> ::
              (for (entry ← entries)
                yield <tr class="bibline">
                        <td class="bibref">{ entry.key }</td>
                        <td class="bibitem">{ render(entry) }</td>
                      </tr>)
          }
        }
      </tbody>
    </table>
  }

  lazy val noTitle = StringValue("Unknown Title")
  lazy val noAuthor = StringValue("Unknown Author")

  def defaultCSS = """/* title */
.bibtitle { font-weight:bold; }
/* author */
.bibauthor { /* nothing by default */ }
/* booktitle (e.g. proceedings title, journal name, etc )*/
.bibbooktitle { font-style:italic; }
/* publisher */
.bibpublisher { /* nothing by default */ }
.group { /* nothing by default */ }

.title {
  color: #003366;
  font-size: large;
  font-weight: bold;
  text-align: right;
}

.header {
  background-color: #995124;
  color: #FFFFFF;
  padding: 1px 2px 1px 2px;
}

.rheader {
  font-weight: bold;
  background-color: #003366;
  color: #ffffff;
  padding: 2px;
  margin-bottom: 10px;
  border-bottom: #ff6633 2px solid;

}
.menu {
  font-size: x-small;
  background-color: #EFDDB4;
  padding: 0px;
  border: 1px solid #000000;
  margin: 0px;
}
.menu a {
  text-decoration: none;
  color: #003366;
}
.menu a:hover {
  color: #ff6633;
}

.bibref {
  padding:7px;
  padding-left:15px;
  vertical-align:text-top; 
}

.result {
  padding:0px;
  border: 1px solid #000000;
  margin:0px;
  background-color: #ffffff;
  width:100%;
}
.result a {
  text-decoration: none;
  color: #469AF8;
}

.result a:hover {
  color: #ff6633;
}

.input_box{
  margin-bottom : 2px;
}
.mini_se {
  border: none 0;
  border-top: 1px dashed #717171;
  height: 1px;
}
.a_name a {
  color:#469AF8;
  width:130px;
}

.rsslink {
  text-decoration: none;
  color:#F88017;
/* could be fancy, see : http://www.feedicons.com/ for icons*/
  /*background-image: url("rss.png"); text-indent: -9999px;*/
}

.purebibtex {
  font-family: monospace;
  font-size: small;
  border: 1px solid #DDDDDD;
  white-space:pre;
  background: none repeat scroll 0 0 #F5F5F5;  
  padding:10px;
}
"""

  /** Renders an article */
  def renderArticle(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      entry.field("journal"),
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a book */
  def renderBook(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").orElse(entry.field("editor")).getOrElse(noAuthor),
      entry.field("edition"),
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a booklet */
  def renderBooklet(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").orElse(entry.field("editor")).getOrElse(noAuthor),
      None,
      None,
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a conference */
  def renderConference(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      entry.field("booktitle"),
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders an inbook */
  def renderInBook(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("chapter").orElse(entry.field("pages")).getOrElse(noTitle),
      entry.field("author").orElse(entry.field("editor")).getOrElse(noAuthor),
      entry.field("title"),
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders an incollection */
  def renderInCollection(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      entry.field("booktitle"),
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders an inproceedings */
  def renderInProceedings(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      entry.field("booktitle"),
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a manual */
  def renderManual(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      None,
      None,
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a masterthesis */
  def renderMasterThesis(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      None,
      entry.field("school"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a misc */
  def renderMisc(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      None,
      None,
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a phdthesis */
  def renderPhdThesis(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      None,
      entry.field("school"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a proceedings */
  def renderProceedings(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("editor").getOrElse(noAuthor),
      None,
      entry.field("publisher"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a techreport */
  def renderTechReport(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      None,
      entry.field("institution"),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders a unpublished */
  def renderUnpublished(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").getOrElse(noAuthor),
      None,
      None,
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  /** Renders an unknown */
  def renderUnknown(entry: BibEntry) =
    renderEntry(entry.key,
      entry.field("title").getOrElse(noTitle),
      entry.field("author").orElse(entry.field("editor")).getOrElse(noAuthor),
      entry.field("booktitle").orElse(entry.field("journal")),
      entry.field("publisher").orElse(entry.field("school")),
      entry.field("year"),
      entry.field("comment"),
      entry.field("url"),
      entry.field("doi"),
      entry.toBibTeX)

  // ====== Helper Methods ======

  override protected[this] lazy val stringFormatter =
    StringFormatters.flatten

  private def renderEntry(key: String,
                          title: Value,
                          author: Value,
                          booktitle: Option[Value],
                          publisher: Option[Value],
                          year: Option[Value],
                          comment: Option[Value],
                          url: Option[Value],
                          doi: Option[Value],
                          bib: String) =
    Group(List(
      <span class="bibtitle">{
        url match {
          case Some(url) ⇒
            <a href={ stringFormatter(url.toString) }>{ stringFormatter(title.toString) }</a>
          case _ ⇒ Text(stringFormatter(title.toString))
        }
      }</span>,
      <span class="bibauthor">{ " (" + formatNames(author.toString) + ")" }</span>,
      {
        Group(List(
          booktitle match {
            case Some(booktitle) ⇒
              <span class="bibbooktitle">{ ", \nIn " + stringFormatter(booktitle.toString) }</span>
            case _ ⇒ Text("")
          },
          publisher match {
            case Some(publisher) ⇒
              <span class="bibpublisher">{ ", \n" + stringFormatter(publisher.toString) }</span>
            case _ ⇒ Text("")
          },
          year match {
            case Some(year) ⇒ Text(", \n" + stringFormatter(year.toString) + ".")
            case _          ⇒ Text(".")
          },
          comment match {
            case Some(comment) ⇒ Text(" (" + stringFormatter(comment.toString) + ")")
            case _             ⇒ Text("")
          },
          <span>
            &nbsp;
            <a href="javascript:void(0)" onclick={
              "if(document.getElementById('bib:" +
                key +
                "').style.display == 'none'){ document.getElementById('bib:" +
                key +
                "').style.display = 'block'; }else{ document.getElementById('bib:" +
                key +
                "').style.display = 'none'; }"
            }>[bib]</a>
          </span>,
          doi match {
            case Some(doi) ⇒ <span>
                               &nbsp;
                               <a href={ "http://dx.doi.org/" + doi }>[doi]</a>
                             </span>
            case _ ⇒ Text("")
          },
          <div id={ "bib:" + key } class="purebibtex" style="display:none">{ bib }</div>))
      }))

}