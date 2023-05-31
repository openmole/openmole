package org.openmole.site.content.documentation

/*
 * Copyright (C) 2023 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Native._

object GUI extends PageContent(html"""

${h2{"Overview"}}

This GUI is an advanced editor for OpenMOLE experiment description through OpenMOLE scripts.
It allows editing, managing and running them.
The way to write these experiments is fully explained in the ${a("main documentation of OpenMOLE", href := DocumentationPages.plug.file)}.
We focus here on the way to manage them in the application.

$br

In the OpenMOLE GUI, you can upload and edit files, run and monitor experiments, and store authentication credentials for distributed computation.
The application runs in a browser (Firefox or Chrome).
The first time you run it, you are asked for a password to encrypt your settings (server port, authentication credentials, etc).
Your settings data (so not your projects, which are never wiped out) are preserved so long as your password do not change.
For now, the OpenMOLE GUI looks like a web application but still runs as a heavy client one.

${img(src := Resource.img.guiGuide.overview.file, width := "100%")}


${h2{"Starting a project"}}

Clicking on the main red button ${i{"New project"}} in the menu bar offers 3 choices:


${h3{"Empty project"}}
Selecting this option creates a new script file called ${i{"newProject.oms"}} in the current folder and open the (empty) file for edition.


${h3{"From Market Place"}}

Selecting this option pops up a dialog box containing all the running projects contained in our Market Place.
The Market Place is a collection of projects built by users which offers different aspects of experiments that can be run on the OpenMOLE platform with a large variety of programming codes, exploration methods.
The sources can be found ${aa("here", href := shared.link.repo.market)}.
Just select one of the Market entries and click on the ${i{"Download"}} button to import it in the current folder.


${h3{"From existing model sources"}}
The model wizard is a tool designed to quickly import your model.
It both uploads your model archive and prepares the OpenMOLE script for you.
The wizard automatically distinguishes your model's programming language among JVM, NetLogo or native codes.
Then it detects the potential inputs and outputs.
For each input/output, the wizard generates a variable with a relevant name if possible.
At the end of the import process, you should be able to run your script without having to do anything else!

$br

To import your model, click on the ${i{"Your Model"}} button.
A dialog box pops up where you can set your model path.
The system should now display the programming language, a list of detected inputs/outputs and the command to launch your code.
In most cases, you are almost done.
Just press the ${i{"Build"}} button at the bottom: the wizard dialog box disappears and the OpenMOLE script is generated in your workDirectory with your uploaded code!
However, you can also make some modifications to input/output names, or launching commands before building the script.

${img(src := Resource.img.guiGuide.modelImport.file, width := "100%")}

For each input/output, three actions can be triggered using the icons located on the same line:
${ul(
    li("removes the current input/output"),
    li("duplicates the current input/output, so that it is both defined as input and output"),
    li("or switches an input to output and vice-versa.")
)}

The launching command uses the names of the previously defined input/output variables.
It is reactive: if the name of the input/output changes, the launching command is updated with the corresponding name.
For the native codes (C, C++, Python, R, ...), the following syntax is required (automatically used): ${code{"${}"}}.

$br

The NetLogo applications working with the ${i{".nls"}} extension should be uploaded as a folder.

${h2{"File Management"}}

The OpenMOLE application essentially handles files: your model files, your model inputs our outputs, and the OpenMOLE scripts,
where you describe your experiment.

$br

We distinguish multiple kinds of resources:
  ${ul(
      li("oms (for Open Mole Script) is a file describing an OpenMOLE experiment according to the OpenMOLE language"),
      li("""
          external code used in OpenMOLE scripts. Codes written in some specific programming languages (Java, Scala, NetLogo, R, Python, ...) can be edited in the application.
           However, they will not be compiled."""),
      li("other external resources used as input for a model are editable in the application (CSV files, text files, ...), while binary files like images cannot be modified.")
  )}

These files are managed in a file system located in the left sidebar.
This side bar offers basic tools for managing files.

${img(src := Resource.img.guiGuide.files.file, width := "90%")}

The current directory is shown at the top in the folder navigation area.
When the folder hierarchy is too deep to fit in the bar, it will be replaced by "...".
Clicking on one folder of the stack sets it as the current folder.
In the image above, the current directory is for example ${i{"Pi Computation"}}.

$br

The tool area at the top concerns the current folder and provides:
${ul(
    li(html"${b{"filtering"}} this folder by number of entries or by names. It is especially recommended for folders containing a large number of files."),
    li(html"${b{"file or folder creation"}} in the current folder."),
    li(html"""
        ${b{"copying files"}}. Clicking this icon make entering in a blue multi-selection mode. Each file selected turns to green.
        The copy button on top permits to copy all of them. Then a ${b{"paste"}} button appears and just waits for being pressed from any other folder you go to."""),
    li(html"""
        ${b{"deleting files"}}. Clicking this icon make entering in a blue multi-selection mode. Each selected file turns to green.
        The red ${b{"Delete"}} button on top allows to wipe them out."""),
    li(html"checking for ${a("plugins", href := DocumentationPages.pluginDevelopment.file)} in the current folder."),
    li(html"${b{"uploading a file"}} from your local machine to the current folder"),
    li(html"${b{"refreshing"}} the content of the current folder")
)}

Then, each file on each line has a settings button allowing to ${b{"clone"}} the current file, @b{download} it, ${b{"edit"}} its name or ${b{"remove"}} it.
Other actions are available depending on the context (ie the file extension):
${ul(
    li(html"${b{"run"}} for .oms file in order to run file without editing it."),
    li(html"${b{"extract"}} for archives in order to produce a directory from an archive file."),
    li(html"${b{"to OMS"}} for code source potentially carried by a Task (.jar, nlogo, .py, etc). It starts the model wizard.")
)}


${h2{"Run and monitor executions"}}

When a ${i{".oms"}} file is edited, a ${b{"Run"}} button appears in the top right corner to start the execution of the workflow.
Once the workflow has been started, the execution panel appears, listing information for each execution on a separate row.
At any time, this execution panel can be closed (without aborting the current runs), and re-opened by clicking on the ${a("running icon", href := "#Overview")}

${img(src := Resource.img.guiGuide.running.file, width := "100%")}

The different statuses of the executions are:
${ul(
    li(html"${b("preparing")}: the execution is getting ready before execution starts"),
    li(html"${b{"running"}}: some jobs are running"),
    li(html"${b{"success"}}: the execution has successfully finished"),
    li(html"${b{"failed"}}: the execution has failed, click on this state to see the errors"),
    li(html"${b{"canceled"}}: the execution has been canceled (by means of the button)")
)}


${h2{"Authentications"}}

In OpenMOLE, the computation load can be delegated to remote ${a("environments", href := DocumentationPages.scale.file)}.
When clicking on the ${a("authentication", href := "#Overview")} icon, a panel appears with the list (initially empty) of all the defined authentications.

$br

To add one authentication, click on the @b{New} button.
The currently supported authentications are:
    ${ul(
        li(html"""
            ${b{"SSH authentication with login and password"}} (any environment accessed by means of SSH).$br
             Set the remote host name and your login on this machine (for example john on blueberry.org),
             as well as your password. Once saved, the authentication will be added to your list (by example: john@blueberry.org)"""),
        li(html"""
            ${b{"SSH authentication with SSH private key"}} (any environment accessed by means of SSH).$br
            Enter the same three settings as for the SSH Password. Now add your SSH private key by clicking on No certificate.
            A random name will be associated to your key. Once saved, the authentication will be added to your list (by example: john@blueberry.org)"""),
        li(html"""
            ${b{"Grid certificate"}} (.p12) for ${aa("Grid Computing", href := shared.link.egi)}$br
            It only requires your EGI certificate file and the associated password. Click on No certificate to select your certificate file. It will be renamed to egi.p12. Note that only one EGI certificate is required (you will not need any other one!)""")
    )}

${img(src := Resource.img.guiGuide.authentication.file, width := "100%")}

An authentication can be removed by clicking on the ${b{"trash bin"}} icon.
An existing authentication can also be edited by clicking on the name of an authentication in the list.

$br

Each time an authentication is added, a check is made on the mentioned environment (for the EGI ones, a list of VOs to be checked can be set in the EGI authentication settings).
If it fails, a red label appears.
When clicking on it, the error stack appears.



${h2{"Plugins"}}

New features can be dynamically inserted in the OpenMOLE platform through plugins.
Advanced users build their own plugins to express concepts that might not be present (yet) in OpenMOLE.
In OpenMOLE, plugins take the form of jar files.
JVM based models can also be provided as plugins.
The way to build plugins in OpenMOLE is fully described ${a("here", href := DocumentationPages.pluginDevelopment.file)}

$br

To add a plugin, open the ${a("plugin management panel", href := "#Overview")}.
You can upload a new plugin by clicking on the blue top right-hand corner and selecting the corresponding jar file.
Once uploaded, the plugin appears in the list.

${img(src := Resource.img.guiGuide.plugin.file, width := "100%")}


""")
