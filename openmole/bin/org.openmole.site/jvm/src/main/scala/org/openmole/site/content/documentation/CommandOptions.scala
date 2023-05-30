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

object CommandOptions extends PageContent(html"""



${h2{"Running OpenMOLE with a Graphical User Interface"}}

The default way to run the OpenMOLE application is with a graphical user interface (GUI).
To do so, just execute the ${code{"openmole"}} file in the folder you ${a("downloaded", href := DocumentationPages.download.file)}, it will bring up the application in your web browser.
OpenMOLE supports Chrome and Firefox, so if you are using another default web browser please copy-paste the OpenMOLE url ${b{"http://localhost:[port]"}} in one of these browsers.

$br

You should see something like below.
The documentation concerning the GUI is provided within the GUI, some basic information can also be found ${a("here", href := gui.file)}.

${img(src := Resource.img.mole.uiScreenshot.file, width := "100%")}

${h3{"GUI on a remote machine"}}

To run OpenMOLE on a remote machine you should execute the following command ${code{"openmole --remote --port portnumber"}}.
The first time you launch it, you will be prompted to choose a password.
Then you can remotely connect to OpenMOLE using the url ${b{"https://remotemachine:portnumber"}} (note that the "https://" part is important).
When you connect to OpenMOLE through your remote, you will be asked for the password you chose previously.


${h2{"Running OpenMOLE in headless mode"}}
OpenMOLE offers a headless mode for running scripts.
You can enable it thanks to the ${code{"-s"}} option: ${code{"./openmole -s /path/to/you/mole/script"}}.

$br

In that case, OpenMOLE still asks for your previous cyphering password.
To provide it at launch time, use the ${code{"-pw"}} option: ${code{"./openmole -s /path/to/your/mole/script --password password"}}.
A better practice is to write this password in a file readable by OpenMOLE only, and use ${code{"./openmole -s /path/to/your/mole/script --password-file password.txt"}}.


${h3{"Interactive console mode"}}

OpenMOLE also offers an interactive console mode.
To launch the console execute ${code{"openmole -c"}} in a console.
The only differences between the scripts in the console mode and the ones from the editor in the GUI are the ways you launch the execution of a workflow, you cancel it, and you follow the execution progress.

$br

A console workflow is launched like this:

${hl.code("""
  val exploration =
    DirectSampling(
      evaluation = myModel on env,
      sampling = mySampling
    )

  val ex = exploration start
""")}

Using the ${code{"ex"}} and the ${code{"env"}} variables created above, you can follow the progress of the execution by using the commands ${code{"print(ex)"}} and ${code{"print(env)"}}.
To cancel the execution you should use ${code{"ex.cancel"}}.


${h2{"Launching options"}}

OpenMOLE comes with several launching options.
Execute ${code{"openmole -h"}} in a terminal to list them all.
""")
