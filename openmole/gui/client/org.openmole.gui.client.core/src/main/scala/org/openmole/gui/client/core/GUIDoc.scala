package org.openmole.gui.client.core

import org.openmole.gui.client.core.EnvironmentErrorPanel.SelectableLevel
import org.openmole.gui.ext.data.{ DebugLevel, ErrorLevel }
import org.openmole.gui.misc.js.BootstrapTags._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, OMTags, ClassKeyAggregator, Select }
import org.scalajs.dom.raw.HTMLDivElement
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom._

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

  val omLanguageLink = a(href := "http://www.openmole.org/current/Documentation_Language.html", target := "_blank")("OpenMOLE language")
  val omPluginLink = a(href := "http://www.openmole.org/current/Documentation_Development_Plugins.html", target := "_blank")("OpenMOLE plugin")
  val omMarketLink = a(href := "http://www.openmole.org/current/Documentation_Market%20Place.html", target := "_blank")("Market Place")
  val omEnvironmentLink = a(href := "http://www.openmole.org/current/Documentation_Language_Environments.html", target := "_blank")("Environment page")
  val githubMarketLink = a(href := "https://github.com/openmole/openmole-market/", target := "_blank")("GitHub page")
  val toStringHookLink = a(href := "http://www.openmole.org/current/Documentation_Language_Hooks.html", target := "_blank")("ToString Hook")

  val rLink = a(href := "https://www.r-project.org/", target := "_blank")("R")
  val netlogoLink = a(href := "https://ccl.northwestern.edu/netlogo/", target := "_blank")("Netlogo")
  val pythonLink = a(href := "https://www.python.org/", target := "_blank")("Python")
  val javaLink = a(href := "http://openjdk.java.net/", target := "_blank")("Java")
  val scalaLink = a(href := "http://www.scala-lang.org/", target := "_blank")("Scala")
  val csvLink = a(href := "https://en.wikipedia.org/wiki/Comma-separated_values", target := "_blank")("CSV")

  val resourcesContent = tags.div(
    "Resources are files, which can be used in OpenMOLE workflows. We distinguish multiple kinds of resources:",
    ul(
      li(b(".oms (for Open Mole Script)"), " is a file describing an OpenMOLE workflow according to the ", omLanguageLink),
      li(b("external code"), " used in OpenMOLE scripts. Codes written with some specific programming languages (", javaLink, ", ", scalaLink, ", ", netlogoLink, ", ", rLink, ", ", pythonLink, ", ...) can be editer in the application. " +
        "Any other types of external codes is also supported but as an immutable binary (C, C++, ...)."),
      li(b("Other external resources"), " used as input for a model are editable in the application (", csvLink, " files, text files, ...), while binary files like images cannot be modified.")
    ),
    "These files are managed in a file system located in the left sidebar. The filesystem can be shown or hidden using the ", glyph(bs.glyph_file + " glyphText"), " button.",
    bs.div("centerImg")(img(src := "img/fileManagement.png", alt := "fileManagement")),
    "The ", bs.span("greenBold")("current directory"), " shown at the top, on the right of the stack. A folder hierarchy too deep to fit in the bar will be replaced by \"...\". " +
      "Clicking on one folder of the stack sets it as the current folder. On the image above, the current directory is for example ", tags.em("SimpopLocal"),
    ".",

    bs.div("spacer20")("Second from the top if a File management utility box. It enables: "),
    ul(
      li("creating a new file or folder in the current directory. To do so, select ", tags.em("file"), " or ", tags.em("folder"), " thanks to the ", bs.span("greenBold")("file or folder selector"), ". Then, type the new name in the ",
        bs.span("greenBold")("file or folder name input"), " and press enter. The freshly created file or folder appears in the list."),
      li(bs.span("greenBold")("uploading a file"), " from your local machine to the current directory"),
      li(bs.span("greenBold")("refreshing"), " the content of the current directory.")
    ),

    bs.div("spacer20")("The content of the current directory is listed at the bottom. Each row gives the name and size of each file or folder. Folders are marked by a ", tags.div(`class` := "dir bottom-5"), ". A ", glyph(bs.glyph_plus), " symbol indicates that the folder is not empty."
    ),
    bs.div("spacer20")("Hovering a file or a folder with the mouse pointer triggers new actions:",
      ul(
        li(glyph(bs.glyph_download + " right2"), " download the hovered file or directory (as an archive for the latter) to the local machine."),
        li(glyph(bs.glyph_edit + " right2"), " rename the hovered file or directory. An input field appears: just type the new name and press ", tags.em("enter"), " to validate."),
        li(glyph(bs.glyph_trash + " right2"), " delete the hovered file or directory."),
        li(glyph(bs.glyph_archive + " right2"), " uncompress the hovered file (appears only in case of archive files (", tags.em(".tgz"), " or ", tags.em(".tar.gz"), ").")
      )),
    bs.div("spacer20")("The editable files can be modified in the central editor. To do so, simply click on the file to be edited.")
  )

  val editionPanelContent = tags.div(
    "The edition panel shows up when you click on a editable file from the resource management panel. Files that can be " +
      "edited or visualised in OpenMOLE are text-based files like OpenMOLE scripts ", tags.em("(.oms)"), " and data sources ", tags.em("(.csv)"),

    bs.div("spacer20")("By default, only OpenMOLE scripts open in editable mode. Other editable files require the edition mode " +
      "to be enabled by clicking on the ", glyph(bs.glyph_edit + " Edit"), " icon. Once a file has been switched to edition mode, it will remain " +
      "in this mode until closed."),
    bs.div("spacer20")("OpenMOLE automatically saves the modifications made to the files opened in the edition panel every 5 seconds. " +
      "It is therefore extremely important not to enable the edition mode of a file that is currently updated. Typically, " +
      "a CSV gathering results from the workflow execution would be overwritten by the editor and lose the precious accumulated results.")
  )

  val executionContent = {
    val logLevels = Seq(ErrorLevel(), DebugLevel())

    tags.div(
      "An .oms script file can be run and monitored via the execution panel ", glyph(bs.glyph_settings + " glyphText"),
      bs.div("spacer20")(tags.b("Monitor an execution:"),
        tags.div("When a .oms file is edited, a ", bs.button("Play", btn_primary + " labelInline"), " appears in the top right corner to start the execution of the workflow." +
          " Once the workflow has been started, the execution panel appears, listing information for each execution on a separate row. " +
          "From left to right, the entries are:",
          tags.ul(
            tags.li("The script name (Ex: explore.oms)"),
            tags.li("The start time of the execution (Ex: 1/9/2015, 15:07:20 )"),
            tags.li(glyph(bs.glyph_flash + " right2"), " the number of running jobs (Ex: ", glyph(bs.glyph_fire + " right2"), " 227)"),
            tags.li(glyph(bs.glyph_flag + " right2"), " the jobs progression with (#finished jobs / # jobs) (Ex: ", glyph(bs.glyph_flag + " right2"), " 17 / 2345)"),
            tags.li("The execution duration (Ex: 1:17:44)"),
            tags.li("The execution state with:",
              tags.ul(
                tags.li(tags.span(attrs.style := "color: yellow; font-weight: bold;", "running"), ": some jobs are running"),
                tags.li(tags.span(attrs.style := "color: #a6bf26; font-weight: bold;", "success"), ": the execution has successfully finished",
                  tags.li(tags.span(attrs.style := "color: #CC3A36; font-weight: bold;", "failed"), ": the execution has failed: click on this state to see the errors"),
                  tags.li(tags.span(attrs.style := "color: orange; font-weight: bold;", "canceled"), ": the execution has been canceled (by means of the ", glyph(bs.glyph_remove + " right2"), " button)")
                ))
            ),
            tags.li(glyph(bs.glyph_stats), "Env gives information about the execution on the remote environments defined in the workflow (See below)"),
            tags.li(glyph(bs.glyph_list + " right2"), " collects the standard output stream. You will find the results of your ", toStringHookLink, "  in this panel, if you defined one in your script"),
            tags.li(glyph(bs.glyph_remove + " right2"), " cancels the execution"),
            tags.li(glyph(bs.glyph_trash + " right2"), " removes the execution from the list.")
          )
        ),
        bs.div("spacer20")(
          "The output history ", bs.input("500", "right2 labelInline")(attrs.style := "color:#333; width : 60px;"), " sets the number of entries from the standard outputs of the executions to be displayed  ( ",
          glyph(bs.glyph_list + " right2"), " ). It is set by default to 500."
        )
      ),
      bs.div("spacer20")(tags.b("Monitor the environments of an execution:"),
        tags.div("When clicking on ", glyph(bs.glyph_stats), "Env, and at least one environment has been defined in the running script, a new line about environment statuses appear with the following information:",
          tags.ul(
            tags.li("The name of the environment. If it has not been named explicitly in the script, it will appear like: LocalEnvironment@1371838186 or GridEnvironment@5718318899"),
            tags.li(glyph(bs.glyph_upload + " right2"), "The number of files and the amount of data uploaded to the remote environment (Ex: 27 (14MB))"),
            tags.li(glyph(bs.glyph_download + " right2"), "The number of files and the amount of data downloaded from the remote environment (Ex: 144 (221KB))"),
            tags.li(glyph(bs.glyph_road + " right2"), "The number of jobs submitted to the remote environment (Ex: ", glyph(bs.glyph_road + " right2"), " 1225)"),
            tags.li(glyph(bs.glyph_flash + " right2"), " the number of jobs running on the remote environment (Ex: ", glyph(bs.glyph_fire + " right2"), " 447)"),
            tags.li(glyph(bs.glyph_flag + " right2"), " the number of jobs finished on the remote environment (Ex: ", glyph(bs.glyph_flag + " right2"), " 127)"),
            tags.li(glyph(bs.glyph_fire + " right2"), " the number of failed jobs on the remote environment (Ex: ", glyph(bs.glyph_fire + " right2"), " 4)"),
            tags.li(tags.a("details"), " is a link to monitor the environment logs. It is useful to diagnose a problem on the environment.")
          )
        ),
        bs.div("spacer20")(
          Select[SelectableLevel]("errorLevelDoc", logLevels.map { level ⇒
            (SelectableLevel(level, level.name), emptyCK)
          }, Some(logLevels.head), btn_primary
          ).selector, " switches the logging verbosity from fine (DEBUG) to minimal (ERROR) for all the environments."
        )
      )
    )
  }

  val authenticationContent = {
    val factories = ClientService.authenticationFactories

    tags.div(
      "In OpenMOLE, the computation load can be delegated to remote environments (remote server through SSH, Cluster, Grid, ...) as explained on the ",
      omEnvironmentLink, ". It is previously necessary to save the connection settings for these different environments (like login/password or ssh key). When clicking on ",
      glyph(bs.glyph_lock + " glyphText"), " a panel appears with the list (initially empty) of all the defined authentications.",
      bs.div("spacer20")(
        "The currently supported authentications are:",
        tags.ul(
          tags.li("SSH authentication with login and password (any environment accessed by means of SSH)"),
          tags.li("SSH authentication with SSH private key (any environment accessed by means of SSH)"),
          tags.li("Grid certificate (.p12) for Grid Computing")
        )
      ),
      bs.div("spacer20")(
        "To add one authentication, click on the ", glyph(bs.glyph_plus + " right2"),
        " icon. In the new panel, select the authentication category: ",
        Select("authentications",
          factories.map { f ⇒ (f, emptyCK) },
          factories.headOption,
          btn_primary
        ).selector
      ), "Your selection updates the available settings. Let's see them in details:",
      bs.div("spacer20")(tags.b("SSH Password:"),
        tags.div("Set the remote host name and your login on this machine (for example john on blueberry.org), as well as your password. Once saved, the authentication will be added to your list (by example: john@blueberry.org)")
      ),
      bs.div("spacer20")(tags.b("SSH Key:"),
        tags.div("Enter the same three settings as for the SSH Password. Now add your SSH private key by clicking on ",
          tags.label(`class` := "inputFileStyle labelInline")("No certificate"),
          ". A random name will be associated to your key. ",
          "Once saved, the authentication will be added to your list (by example: john@blueberry.org)")
      ),
      bs.div("spacer20")(tags.b("EGI P12 Certificate:"),
        tags.div("It only requires your EGI certificate file and the associated password. Click on ",
          tags.label(`class` := "inputFileStyle labelInline")("No certificate"),
          " to select your certificate file. It will be renamed to egi.p12. Note that only one EGI certificate is required (you will not need any other one!)"
        ),
        bs.div("spacer20")("Remove an existing authentication by clicking on the ", glyph(bs.glyph_trash + " right2"), " (visible when hovering an  authentication in the list). An existing authentication can also be edited by clicking on the name of an authentication in the list."
        )
      )
    )
  }

  val marketContent = tags.div(
    "The Market Place gathers turnkey working examples of OpenMOLE workflow. They are excellent starting points for building your own project. All the examples in the market place provide:",
    tags.ul(
      tags.li("At least a .oms file containing an executable workflow script,"),
      tags.li("The embedded application (except for a small Scala code, which can be contained in an Scala Task),"),
      tags.li("A README.md describing the model or the method used in the example.")
    ),
    bs.div("spacer20")(
      "All the examples from the market can be found in the ", omMarketLink, " of the website and in the application by clicking ", glyph(bs.glyph_market + " glyphText"),
      ". From the list of market entries, you can read the README.md by clicking on the name of the example. Download the whole project to your current working directory by pressing the ",
      tags.em("Download"), " button. You can now open the project's .oms file and press play to start its execution.",
      bs.div("spacer20")("Browse the sources and propose your own project on the dedicated ", githubMarketLink, ".")
    )

  )

  val pluginContent = tags.div(
    "New features can be dynamically inserted in The OpenMOLE platform through plugins. Advanced users build their own" +
      "plugins to express concepts that might not be present (yet) in OpenMOLE. In OpenMOLE, plugins take the form of ", tags.em(" jar"),
    " files.",
    bs.div("spacer20")("Open the plugin management panel by clicking on ", glyph(OMTags.glyph_plug + " glyphText"), ". You can upload a new plugin by clicking on ", bs.glyph(bs.glyph_upload + " right2"), " and selecting the corresponding jar file. "),
    bs.div("spacer20")("Once uploaded, the plugin appears in the list. Hover a plugin in the list to display  the ", glyph(bs.glyph_trash + " right2"), " icon and remove the selected plugin from your selection."),
    bs.div("spacer20")("More information about plugins can be found in the ", omPluginLink, " section of the website. Plugins are added, the "
    )
  )

  val entries = Seq(
    GUIDocEntry(bs.glyph_file, "Manage resources", resourcesContent),
    GUIDocEntry(bs.glyph_edit, "Edition panel", editionPanelContent),
    GUIDocEntry(bs.glyph_settings, "Execute scripts", executionContent),
    GUIDocEntry(bs.glyph_lock, "Manage authentications", authenticationContent),
    GUIDocEntry(bs.glyph_market, "Market place", marketContent),
    GUIDocEntry(OMTags.glyph_plug, "Plugins", pluginContent)
  )

  val doc: TypedTag[HTMLDivElement] = {
    tags.div(`class` := "docText",
      "This application is an advanced editor for OpenMOLE scripts. It allows editing, managing and running them." +
        " The description of the ", omLanguageLink, " and concepts can be found on the OpenMOLE website.",
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