package org.openmole.gui.client.core

/*
 * Copyright (C) 04/07/16 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.misc.js.OMTags
import org.scalajs.dom.raw.HTMLDivElement

import scalatags.JsDom.tags
import scalatags.JsDom.TypedTag
import org.openmole.gui.misc.js.JsRxTags._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import org.openmole.gui.client.core.authentications.AuthPanelWithID
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom._
import omsheet._
import sheet._

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

class GUIDocPanel extends ModalPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  lazy val modalID = "documentationID"

  def onOpen() = {}

  def onClose() = {}

  case class GUIDocEntry(glyph: ModifierSeq, title: String, content: TypedTag[HTMLDivElement])

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

  val glyphStyle = sheet.paddingRight(2)
  val spacer20 = sheet.marginTop(20)
  val bold = fontWeight := "bold"

  val resourcesContent = tags.div(
    "Resources are files, which can be used in OpenMOLE workflows. We distinguish multiple kinds of resources:",
    ul(
      li(b(".oms (for Open Mole Script)"), " is a file describing an OpenMOLE workflow according to the ", omLanguageLink),
      li(b("external code"), " used in OpenMOLE scripts. Codes written with some specific programming languages (", javaLink, ", ", scalaLink, ", ", netlogoLink, ", ", rLink, ", ", pythonLink, ", ...) can be editer in the application. " +
        "Any other types of external codes is also supported but as an immutable binary (C, C++, ...)."),
      li(b("Other external resources"), " used as input for a model are editable in the application (", csvLink, " files, text files, ...), while binary files like images cannot be modified.")
    ),
    "These files are managed in a file system located in the left sidebar. The filesystem can be shown or hidden using the ", span(glyph_file +++ glyphText), " button.",
    div(centerElement, img(src := "img/files.png", alt := "files")),
    "The ", span(omsheet.greenBold, "current directory"), " shown at the top, on the right of the stack. A folder hierarchy too deep to fit in the bar will be replaced by \"...\". " +
      "Clicking on one folder of the stack sets it as the current folder. On the image above, the current directory is for example ", tags.em("SimpopLocal"), ".",
    div(spacer20, "Second from the top if a File management utility box. It enables: "),
    ul(
      li("creating a new file or folder in the current directory. To do so, select ", tags.em("file"), " or ", tags.em("folder"), " thanks to the ", span(greenBold, "file or folder selector"), ". Then, type the new name in the ",
        span(greenBold, "file or folder name input"), " and press enter. The freshly created file or folder appears in the list."),
      li(span(greenBold, "uploading a file"), " from your local machine to the current directory"),
      li(span(greenBold, "refreshing"), " the content of the current directory.")
    ),

    div(spacer20, "The content of the current directory is listed at the bottom. Each row gives the name and size of each file or folder. Folders are marked by a ", tags.div(ms("dir bottom-5")), ". A ", span(glyph_plus), " symbol indicates that the folder is not empty."),
    div(
      spacer20,
      "Hovering a file or a folder with the mouse pointer triggers new actions:",
      ul(
        li(span(glyph_download +++ glyphStyle), " download the hovered file or directory (as an archive for the latter) to the local machine."),
        li(span(glyph_edit +++ glyphStyle), " rename the hovered file or directory. An input field appears: just type the new name and press ", tags.em("enter"), " to validate."),
        li(span(glyph_trash +++ glyphStyle), " delete the hovered file or directory."),
        li(span(glyph_archive +++ glyphStyle), " uncompress the hovered file (appears only in case of archive files (", tags.em(".tgz"), " or ", tags.em(".tar.gz"), ").")
      )
    ),
    div(spacer20, "The editable files can be modified in the central editor. To do so, simply click on the file to be edited."),
    fileToolsContent
  )

  val editionPanelContent = tags.div(
    "The edition panel shows up when you click on a editable file from the resource management panel. Files that can be " +
      "edited or visualised in OpenMOLE are text-based files like OpenMOLE scripts ", tags.em("(.oms)"), " and data sources ", tags.em("(.csv)"),

    div(spacer20, "By default, only OpenMOLE scripts open in editable mode. Other editable files require the edition mode " +
      "to be enabled by clicking on the ", span(glyph_edit, " Edit"), " icon. Once a file has been switched to edition mode, it will remain " +
      "in this mode until closed."),
    div(spacer20, "OpenMOLE automatically saves the modifications made to the files opened in the edition panel every 5 seconds. " +
      "It is therefore extremely important not to enable the edition mode of a file that is currently updated. Typically, " +
      "a CSV gathering results from the workflow execution would be overwritten by the editor and lose the precious accumulated results.")
  )

  val modelWizardContent = {
    tags.div(
      "The model wizard is a tool designed to quickly import your Model. It both uploads your model archive and prepares for you the OpenMOLE script to run it." +
        "The wizard automatically detects your model programming language among JVM, Netlogo or native codes. Then it detects the potential inputs and outputs. It generates " +
        "for each input/output a variable with a relevant name if possible. At the end of the import process, the ", button("Build", btn_primary +++ labelInLine), " should run your script !",
      tags.div(spacer20, "To import your model, click on the ", button("Your model", btn_primary +++ labelInLine), " button. A dialog box pops up. Set your model path in it. The system should now display " +
        "the programming language, a list of detected inputs / outputs and the script command to launch your code. In many cases, you are almost done yet. Just press the ",
        tags.b("Build"), " button at the bottom: the wizard dialog disappears and the OpenMOLE script is generated in the wordDirectory with your uploaded code !"),
      tags.div(
        spacer20,
        "However, you may sometimes make some modifications if you observe the system did not correctly detect your code, its inputs/outputs or its launching command. " +
          "For each input / output, three actions can be triggered thanks to icons located on the same line: ",
        tags.ul(
          tags.li(span(glyph_trash +++ glyphStyle), " removes the current input/output"),
          tags.li(span(glyph_arrow_right_and_left +++ glyphStyle), " duplicates the current input/output, so that it is both defined as input and output"),
          tags.li(span(glyph_arrow_right +++ glyphStyle), " or ", span(glyph_arrow_left +++ glyphStyle), " switches an input to output or an input to output")
        ),
        "The launching command uses the names of the previously defined input / output variables. It is reactive: if the name of input/output is changed, the launching command" +
          " is changed with the corresponding name. For the native codes (C, C++, Python, R, ...), the following syntax is required (automatically used): ", tags.b("""${}."""),
        tags.div(spacer20 +++ bold, "Netlogo applications"),
        tags.div(" The Netlogo applications working with nls extensions should be previously archived. The system will exstract the archives and deal with the extensions as " +
          " Task resources (they appear in the resources tab"),
        tags.div("A seed option (for the Netlogo random genarator) is proposed to automatically generate a seed variable and provhide it to the Netlogo Task"),
        tags.div(spacer20 +++ bold, "Java applications"),
        tags.div(" Once the jar has been provided to the wizard, the classes contained in it are proposed in a dropdown button. Choose the class containing your main Class. " +
          "Then, another dropdown button will ask you for the function to be called in the preselected class.")
      )
    )
  }

  lazy val fileToolsContent = {
    tags.div(
      spacer20,
      "Several options on top help you to manage your files",
      tags.ul(
        tags.li(span(glyph_filter +++ glyphStyle), "Filtering: a directory can potentially contain a lot of files. By default the first 100 are displayed to prevent from network latency. However, you can change" +
          " this value. You can also filter by name (and then press enter). The 3 icons on the top of the file list will then permit to sort results respectively by names ", span(glyph_alph_sorting +++ glyphStyle), " , file creation times ", span(glyph_time +++ glyphStyle), " or size ", span(OMTags.glyph_data +++ glyphStyle, " .")),
        tags.li(span(glyph_plus +++ glyphStyle), "Create new file or directory: the dropdown button permits to choose among file or directory. Set a name and then press enter. A file or a directory will be " +
          "generated in the workDirectory."),
        tags.li(span(glyph_copy +++ glyphStyle), " multiple copy / paste: each row is now selectable (grey color). Once you select one, it turns to green. Then press the copy button (on the tool area left side) to copy them ",
          tags.label("paste")(label_danger +++ pasteLabel +++ labelStyle +++ (top := 0)), " appears. Navigate to the target destination and press the button to complete the paste action."),
        tags.li(span(glyph_trash +++ glyphStyle), " multiple deletion: each row will be selectable (grey color). Once you select one, it turns to red. Then press the delete button (on the tool area left side) to remove them."),
        tags.li(span(OMTags.glyph_plug +++ glyphStyle), "Plugins: scan all the files contained in the workDirectory and test whether it is an OpenMOLE plugin or not. If it is a plugin " +
          ", its row appear in grey and can be selected. Click on it to select it and then on the button Plug, which poped up to activate the plugin (you will then find it in the plugin list)."),
        tags.li(span(glyph_upload +++ glyphStyle), " upload a file in the workDirectory."),
        tags.li(span(glyph_refresh +++ glyphStyle), " refresh the workDirectory.")
      )
    )
  }

  val executionContent = {

    tags.div(
      "An .oms script file can be run and monitored via the execution panel ", span(glyph_settings +++ glyphText),
      div(
        spacer20,
        tags.b("Monitor an execution:"),
        tags.div("When a .oms file is edited, a ", button("Play", btn_primary +++ labelInLine), " appears in the top right corner to start the execution of the workflow." +
          " Once the workflow has been started, the execution panel appears, listing information for each execution on a separate row. " +
          "From left to right, the entries are:",
          tags.ul(
            tags.li("The script name (Ex: explore.oms)"),
            tags.li("The start time of the execution (Ex: 1/9/2015, 15:07:20 )"),
            tags.li(span(glyph_flash +++ glyphStyle), " the number of running jobs (Ex: ", span(glyph_fire +++ glyphStyle), " 227)"),
            tags.li(span(glyph_flag +++ glyphStyle), " the jobs progression with (#finished jobs / # jobs) (Ex: ", span(glyph_flag +++ glyphStyle), " 17 / 2345)"),
            tags.li("The execution duration (Ex: 1:17:44)"),
            tags.li(
              "The execution state with:",
              tags.ul(
                tags.li(tags.span(attrs.style := "color: yellow; font-weight: bold;", "running"), ": some jobs are running"),
                tags.li(tags.span(attrs.style := "color: #a6bf26; font-weight: bold;", "success"), ": the execution has successfully finished",
                  tags.li(tags.span(attrs.style := "color: #CC3A36; font-weight: bold;", "failed"), ": the execution has failed: click on this state to see the errors"),
                  tags.li(tags.span(attrs.style := "color: orange; font-weight: bold;", "canceled"), ": the execution has been canceled (by means of the ", span(glyph_remove +++ glyphStyle), " button)"))
              )
            ),
            tags.li(span(glyph_stats), "Env gives information about the execution on the remote environments defined in the workflow (See below)"),
            tags.li(span(glyph_list +++ glyphStyle), " collects the standard output stream. You will find the results of your ", toStringHookLink, "  in this panel, if you defined one in your script"),
            tags.li(span(glyph_remove +++ glyphStyle), " cancels the execution"),
            tags.li(span(glyph_trash +++ glyphStyle), " removes the execution from the list.")
          )),
        div(
          spacer20,
          "The output history ", input("500", glyphStyle +++ labelInLine)(attrs.style := "color:#333; width : 60px;"), " sets the number of entries from the standard outputs of the executions to be displayed  ( ",
          span(glyph_list +++ glyphStyle), " ). It is set by default to 500."
        )
      ),
      div(
        spacer20,
        tags.b("Monitor the environments of an execution:"),
        tags.div("When clicking on ", span(glyph_stats), "Env, and at least one environment has been defined in the running script, a new line about environment statuses appear with the following information:",
          tags.ul(
            tags.li("The name of the environment. If it has not been named explicitly in the script, it will appear like: LocalEnvironment@1371838186 or GridEnvironment@5718318899"),
            tags.li(span(glyph_upload +++ glyphStyle), "The number of files and the amount of data uploaded to the remote environment (Ex: 27 (14MB))"),
            tags.li(span(glyph_download +++ glyphStyle), "The number of files and the amount of data downloaded from the remote environment (Ex: 144 (221KB))"),
            tags.li(span(glyph_road +++ glyphStyle), "The number of jobs submitted to the remote environment (Ex: ", span(glyph_road +++ glyphStyle), " 1225)"),
            tags.li(span(glyph_flash +++ glyphStyle), " the number of jobs running on the remote environment (Ex: ", span(glyph_fire +++ glyphStyle), " 447)"),
            tags.li(span(glyph_flag +++ glyphStyle), " the number of jobs finished on the remote environment (Ex: ", span(glyph_flag +++ glyphStyle), " 127)"),
            tags.li(span(glyph_fire +++ glyphStyle), " the number of failed jobs on the remote environment (Ex: ", span(glyph_fire +++ glyphStyle), " 4)"),
            tags.li(tags.a("details"), " is a link to monitor the environment logs. It is useful to diagnose a problem on the environment.")
          ))
      )
    )
  }

  val authenticationContent = {
    val factories = authentications.factories

    tags.div(
      "In OpenMOLE, the computation load can be delegated to remote environments (remote server through SSH, Cluster, Grid, ...) as explained on the ",
      omEnvironmentLink, ". It is previously necessary to save the connection settings for these different environments (like login/password or ssh key). When clicking on ",
      span(glyph_lock +++ glyphText), " a panel appears with the list (initially empty) of all the defined authentications.",
      div(
        spacer20,
        "The currently supported authentications are:",
        tags.ul(
          tags.li("SSH authentication with login and password (any environment accessed by means of SSH)"),
          tags.li("SSH authentication with SSH private key (any environment accessed by means of SSH)"),
          tags.li("Grid certificate (.p12) for Grid Computing")
        )
      ),
      div(
        spacer20,
        "To add one authentication, click on the ", span(glyph_plus +++ glyphStyle),
        " icon. In the new panel, select the authentication category: ",
        factories.select(
          factories.headOption,
          (auth: AuthPanelWithID) ⇒ auth.name,
          btn_primary,
          onclickExtra = () ⇒ {}
        ).selector
      ),
      "Your selection updates the available settings. Let's see them in details:",
      div(
        spacer20,
        tags.b("SSH Password:"),
        tags.div("Set the remote host name and your login on this machine (for example john on blueberry.org), as well as your password. Once saved, the authentication will be added to your list (by example: john@blueberry.org)")
      ),
      div(
        spacer20,
        tags.b("SSH Key:"),
        tags.div(
          "Enter the same three settings as for the SSH Password. Now add your SSH private key by clicking on ",
          tags.label(ms("inputFileStyle") +++ labelInLine)("No certificate"),
          ". A random name will be associated to your key. ",
          "Once saved, the authentication will be added to your list (by example: john@blueberry.org)"
        )
      ),
      div(
        spacer20,
        tags.b("EGI P12 Certificate:"),
        tags.div(
          "It only requires your EGI certificate file and the associated password. Click on ",
          tags.label(ms("inputFileStyle") +++ labelInLine)("No certificate"),
          " to select your certificate file. It will be renamed to egi.p12. Note that only one EGI certificate is required (you will not need any other one!)"
        ),
        div(spacer20, "Remove an existing authentication by clicking on the ", span(glyph_trash +++ glyphStyle), " (visible when hovering an  authentication in the list). An existing authentication can also be edited by clicking on the name of an authentication in the list.")
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
    div(
      spacer20,
      "All the examples from the market can be found in the ", omMarketLink, " of the website and in the application by clicking ", span(glyph_market +++ glyphText),
      ". From the list of market entries, you can read the README.md by clicking on the name of the example. Download the whole project to your current working directory by pressing the ",
      tags.em("Download"), " button. You can now open the project's .oms file and press play to start its execution.",
      div(spacer20, "Browse the sources and propose your own project on the dedicated ", githubMarketLink, ".")
    )

  )

  val pluginContent = tags.div(
    "New features can be dynamically inserted in The OpenMOLE platform through plugins. Advanced users build their own" +
      "plugins to express concepts that might not be present (yet) in OpenMOLE. In OpenMOLE, plugins take the form of ", tags.em(" jar"),
    " files.",
    div(spacer20, "Open the plugin management panel by clicking on ", span(OMTags.glyph_plug +++ glyphText), ". You can upload a new plugin by clicking on ", span(glyph_upload +++ glyphStyle), " and selecting the corresponding jar file. "),
    div(spacer20, "Once uploaded, the plugin appears in the list. Hover a plugin in the list to display  the ", span(glyph_trash +++ glyphStyle), " icon and remove the selected plugin from your selection."),
    div(spacer20, "More information about plugins can be found in the ", omPluginLink, " section of the website. Plugins are added, the ")
  )

  val entries = Seq(
    GUIDocEntry(glyph_file, "Manage resources", resourcesContent),
    GUIDocEntry(glyph_edit, "Edition panel", editionPanelContent),
    GUIDocEntry(glyph_upload_alt, "Model import", modelWizardContent),
    GUIDocEntry(glyph_settings, "Execute scripts", executionContent),
    GUIDocEntry(glyph_lock, "Manage authentications", authenticationContent),
    GUIDocEntry(glyph_market, "Market place", marketContent),
    GUIDocEntry(OMTags.glyph_plug, "Plugins", pluginContent)
  )

  lazy val dialog = bs.modalDialog(
    modalID,
    bs.headerDialog(
      div(height := 55)(
        b("Documentation")
      )
    ),
    bs.bodyDialog(
      tags.div(
        Rx {
          tags.div(
            omsheet.color("#444"),
            "This application is an advanced editor for OpenMOLE scripts. It allows editing, managing and running them." +
              " The description of the ", omLanguageLink, " and concepts can be found on the OpenMOLE website.",
            for (entry ← entries) yield {
              val isSelected = selectedEntry() == Some(entry)
              tags.div(
                docEntry,
                tags.span(
                  docTitleEntry,
                  onclick := { () ⇒
                    {
                      selectedEntry() = {
                        if (isSelected) None
                        else Some(entry)
                      }
                    }
                  },
                  span(entry.glyph +++ glyphText),
                  entry.title
                ),
                if (isSelected) tags.div(docContent, entry.content)
                else tags.div
              )
            }
          )
        }
      )
    ),
    bs.footerDialog(closeButton)
  )
}
