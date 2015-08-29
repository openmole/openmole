package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.TreeNodePanel
import org.scalajs.dom.raw.{ HTMLInputElement, HTMLDivElement }

import scalatags.JsDom.TypedTag
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, ClassKeyAggregator }
import scalatags.JsDom.all._
import bs._
import rx._

/*
 * Copyright (C) 27/08/15 // mathieu.leclaire@openmole.org
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

object GUIDoc {

  case class GUIDocEntry(glyph: ClassKeyAggregator, title: String, content: TypedTag[HTMLDivElement])

  val selectedEntry: Var[Option[GUIDocEntry]] = Var(None)

  val omLangageLink = a(href := "http://www.openmole.org/current/Documentation_Language.html", target := "_blank")("OpenMOLE language")
  val omPluginLink = a(href := "http://www.openmole.org/current/Documentation_Development_Plugins.html", target := "_blank")("OpenMOLE plugin")

  val rLink = a(href := "https://www.r-project.org/", target := "_blank")("R")
  val netlogoLink = a(href := "https://ccl.northwestern.edu/netlogo/", target := "_blank")("Netlogo")
  val pythonLink = a(href := "https://www.python.org/", target := "_blank")("Python")
  val javaLink = a(href := "http://openjdk.java.net/", target := "_blank")("Java")
  val scalaLink = a(href := "http://www.scala-lang.org/", target := "_blank")("Scala")
  val csvLink = a(href := "https://en.wikipedia.org/wiki/Comma-separated_values", target := "_blank")("CSV")

  val resourcesContent = tags.div(
    "The resources are files, that can be used in OpenMOLE simulations:",
    ul(
      li(b(".oms for Open Mole Script"), "is a file describing an OpenMOLE workflow according the its ", omLangageLink),
      li(b("external code"), " run used in OpenMOLE scripts: editable in the application (", javaLink, ", ", scalaLink, ", ", netlogoLink, ", ", rLink, ", ", pythonLink, ", ...) or not (C, C++, or any binary code)"),
      li(b("Any external resources"), " used as input for a model editable in the application (", csvLink, " files, text files, ...) or not (pictures, or any binary files)")
    ),
    "These files are managed in a file system located in left hand side and that can be showed or hidden thanks to the ", glyph(bs.glyph_file + " glyphText"), " icon",
    bs.div("centerImg")(img(src := "img/fileManagement.png", alt := "fileManagement")),
    "The ", bs.span("greenBold")("current directory"), " path is stacked in the top. The current directory is on the right of the stack. Clicking on one folder of the stack set this folder as current. On the image above, the current directory is for example ", tags.em("SimpopLocal"),
    ".",
    bs.div("spacer20")("The content of the current directory is listed in the bottom. Each row gives the name and the size of each file or folder. A folder is preceded by a ", tags.div(`class` := "dir bottom-5"), ". A ", glyph(bs.glyph_plus), " inside means that the folder is not empty.",
      "In between, a file managment tool enable to: ",
      ul(
        li("create a new file or folder in the current directory. To do so, select ", tags.em("file"), " or ", tags.em("folder"), " thanks to the ", bs.span("greenBold")("file or folder selector"), ". Then, type the required name in the ",
          bs.span("greenBold")("file or folder name input"), " and press enter. The freshly created file or folder appears in the list."),
        li(bs.span("greenBold")("upload a file"), " in the current directory"),
        li(bs.span("greenBold")("Refresh"), " the content of the current directory.")
      )),
    bs.div("spacer20")("When a file or a folder row is hovered, new options appear:",
      ul(
        li(glyph(bs.glyph_download + " right2"), " for downloading the current file or directory (as an archive for the latter)."),
        li(glyph(bs.glyph_edit + " right2"), " for renaming the current file or directory. An input field appears; just input the new name and press " + tags.em("enter") + " to validate."),
        li(glyph(bs.glyph_trash + " right2"), " for deleting the current file or direcotry."),
        li(glyph(bs.glyph_archive + " right2"), " for uncompressing the current file (appears only in case of archive files (", tags.em(".tgz"), " or ", tags.em("tar.gz"), ").")
      )),
    bs.div("spacer20")("The editable files can be modified in the central editor. To do so, simply click on the file to be edited.")
  )

  val pluginContent = tags.div(
    "The OpenMOLE platform is pluggable, meaning that you can build your own extension for any concept. It is however an advanced way of using the platform, so that you probably do not need it.",
    bs.div("spacer20")("All the documentation about plugins can be found on the ",
      omPluginLink, " section on the website. Nethertheless, the ", glyph(bs.glyph_plug + " glyphText"), " section enable to provide your plugins as ", tags.em(" jar"), " file, so that they can be found at execution time if it is used in an OpenMOLE script."
    ), bs.div("spacer20")("To do so, simply click on the ", bs.glyph(bs.glyph_upload + " right2"), " in the plugin panel and navigate to your jar file. " +
      "Once uploaded, the file appears in the list above. Hovering a row in this list makes appear the ", glyph(bs.glyph_trash + " right2"), " icon to remove this plugin from your selection."
    )
  )

  val entries = Seq(
    GUIDocEntry(bs.glyph_file, "Manage the resources", resourcesContent),
    GUIDocEntry(bs.glyph_settings, "Execute scripts", tags.div("Execute")),
    GUIDocEntry(bs.glyph_lock, "Manage authentications", tags.div("authentications")),
    GUIDocEntry(bs.glyph_market, "The Market place", tags.div("market place")),
    GUIDocEntry(bs.glyph_plug, "Plugins", pluginContent)
  )

  val doc: TypedTag[HTMLDivElement] = {
    tags.div(`class` := "docText",
      "This help provides informations to use the OpenMOLE web application.  It looks very much to a web application even if, for now, both server and client run locally.",
      tags.div(
        "This application enables to manage OpenMOLE scripts, to edit them and to run them." +
          " It does not explain how to build a workflow by means of the ",
        omLangageLink,
        ", which is explained in detail on the OpenMOLE website."),
      for (entry ← entries) yield {
        Rx {
          val isSelected = selectedEntry() == Some(entry)
          tags.div(
            `class` := "docEntry" + {
              if (isSelected) " docEntrySelected" else ""
            },
            tags.span(
              `class` := "docTitleEntry",
              onclick := { () ⇒
                {
                  selectedEntry() = {
                    if (isSelected) None
                    else Some(entry)
                  }
                }
              },
              glyph(entry.glyph + "glyphText"),
              entry.title
            ),
            if (isSelected) tags.div(`class` := "docContent", entry.content)
            else tags.div

          )
        }
      }
    )

  }

}