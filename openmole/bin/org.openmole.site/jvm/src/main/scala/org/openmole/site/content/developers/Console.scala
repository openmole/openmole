package org.openmole.site.content.developers

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

import org.openmole.site.content.header.*

object Console extends PageContent(html"""


The ${a("OpenMOLE GUI", href := DocumentationPages.gui.file)} implements all the OpenMOLE features.
However you may be interested in using OpenMOLE in interactive console mode.
To do so, use the ${code{"-c"}} argument in your console: ${code{"./openmole -c"}}.

$br

The only difference between the scripts in the console mode and the ones from the editor is the way you launch their execution, you cancel it, and you follow their progress.
A console workflow is launched as follow:

${hl.code("""val ex = exploration -< (model on env) start""")}

Using the ${code{"ex"}} and the ${code{"env"}} variables, you can follow the progress of the execution by using the commands: ${code{"print(ex)"}} and ${code{"print(env)"}}.
To cancel the execution, you should use ${code{"ex.cancel"}}.

${h2{"Authentications"}}

In console mode, you can define an authentication using a pair of login/password with the following command:

${hl.openmole("""
  SSHAuthentication += LoginPassword("login", password, "machine-name")
""".stripMargin, header = """def password = "" """)}

Or, to authenticate with a private key:

${hl.openmole("""
  SSHAuthentication += PrivateKey("/path/to/the/private/key", "login", password, "machine-name")
""", header = """def password = "" """)}

It mentions the ${i{"password"}} function.
This function will prompt for the password/passphrase of the private key, right after the call to the ${code{"Environment"}} builder, using ${code{"SSHAuthentication"}}.

$br

The last part of ${code{"SSHAuthentication"}}, ${code{"machine-name"}}, should match exactly the address of the machine in your execution environment.
OpenMOLE searches the matching SSH keys using an ${b{"exact match"}} on ${code{"login"}} and ${code{"machine-name"}} between the environment and the stored keys.


${h2{"Run script"}}

In console mode, you have to copy-paste your whole workflow to run it.

A console workflow is launched like this:
${hl.code("""
val ex = exploration -< (model on env) start
""")}

Note that you need to invoke ${code{"start"}} on your workflow, contrary to Editor mode.

$br

Using the ${code{"ex"}} and the ${code{"env"}} variables you can follow the progress of the execution by using the commands:
${code{"print(ex)"}} and ${code{"print(env)"}}.
To cancel the execution you should use ${code{"ex.cancel"}}.

""")
