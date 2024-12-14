package org.openmole.site.content

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
import org.openmole.site.content.Native._

def FAQ =  PageContent(html"""

${h2{"Which Java version should I use?"}}

OpenMOLE is fully working under ${b{"OpenJDK 21 and higher"}}, thus it is our recommended option.
You can check which Java version you're running by typing ${code{"java -version"}} in a console.


${h2{"Where do I find previous versions of OpenMOLE?"}}

Previous versions of the OpenMOLE application and documentation are available ${a("here", href := shared.link.allOpenMOLE)}.
Due to a data loss in 2016, only the versions from OpenMOLE 6 are available.

${h2{"Why is my SSH authentication not working?"}}

When one of the SSH authentications you've added to OpenMOLE is marked as failed, you can try these few steps to identify the problem.

${h3{"Console mode"}}

If you are using OpenMOLE in console mode, try enabling the ${i{"FINE"}} level of logging in the console using: ${code{"logger.level(\"FINE\")"}}.


${h3{"Password authentication"}}

If you are using the ${code{"LoginPassword"}} authentication you might want to double check the user and password you entered since one of them is more than likely incorrect.


${h3{"SSH Keypair Authentication"}}

In such a case, we'll have to investigate multiple options, as SSH public key authentications are sensitive to several configuration parameters.

$br

Public key authentication usually has a higher priority than password-based authentication when trying to connect to a remote server.
Thus, when you attempt an SSH connection to the target environment, if your client asks you to enter a password (please note that a passphrase is different from a password), then your public key authentication is not taken into account.
SSH will skip your public key in case of bad configuration.
The most common cases of badly configured keypairs are the following:
${ul(
  li{html"You haven't created an SSH keypair yet (using ${code{"ssh-keygen"}}). Private keys are usually stored in ${i{"/home/login/.ssh/id_rsa"}} or ${i{"/home/login/.ssh/id_dsa"}}, and should have a matching ${i{"/home/login/.ssh/id_[rd]sa.pub"}} next to them. You can find additional info on how to create an SSH public key ${aa("here", href := shared.link.sshPublicKey)}."},
  li{html"Permissions of your ${i{"/home/login/.ssh"}} folder ${b{"must"}} be set to ${i{"drwx——"}} ${i("(700 in octal)")}. Also, too permissive home directories (with write access given to the whole group for instance) might prove problematic."},
  li{html"A ${i{"/home/login/.ssh/authorized_keys"}} file must be present on the remote system. It should at least contain a line matching the content of the ${i{"/home/login/.ssh/id_[rd]sa.pub"}} from your base system."},
  li{"If you entered a passphrase when you generated your SSH keys and cannot remember it, it might be better to generate another keypair."}
)}

If you still could not solve your SSH authentication problems, another option is to recreate a public/private keypair using the ${hl("ssh-keygen", "bash")} shell command.
Store it in a different file to avoid overwriting the already existing one.
You might also want to try a simple ${code{"LoginPassword"}} authentication as explained in the ${a("SSH page", href := ssh.file)}.

$br

Adding the ${hl("-vvv", "bash")} flag to your ${code{"ssh"}} command will give a lot more details on the communication between your client and the remote server.
This will allow you to find out which authentication is successful as well as the order in which the authentication modes are tried.



${h2{"Is OpenMOLE doing something?"}}

If you think OpenMOLE is crashed or stuck for some reason, here are a few things you can check to decide whether it's just a temporary slow down or if the platform did actually crash.


${h3{"Using tools from the Java Development Kit"}}

A simple call to ${code{"jps"}} from your command line will list all the instrumented JVMs on your system.
If OpenMOLE is running, it will be among these processes.
Now that you know the OpenMOLE's process ID, you can use ${code{"jstack"}} to print the eventual stack traces collected from OpenMOLE's threads.
It's a bit low level but can at least give you enough material to thoroughly document your problem in the ${aa("issue list", href := shared.link.issue)} or the ${aa("forum", href := shared.link.forum)}.
The same procedure can be applied to the ${code{"dbserver"}} running along OpenMOLE to manage the replica of the files copied to execution environments.


${h3{"Inspecting the temporary folders"}}

OpenMOLE automatically creates temporary folders on the machine it's running on, in order to handle various inputs and outputs.
If you have access to the machine running OpenMOLE, change to your OpenMOLE's preferences folder down to the following path: ${i{"/home/user/.openmole/my_machine/.tmp"}}.
List the content of this directory and change to the most recently created directory.

$br

If you're using a remote environment, it should contain the tar archives used to populate new jobs on your remote computing environment, along with the input data files required by the task.
The presence of these files is a good indicator that OpenMOLE is functioning correctly and preparing the delegation of your workflow.
Hardcore debuggers might want to go even deeper and extract the content of the tar archives to verify them, but this is out of scope.
However, touching on temporary file creation in OpenMOLE seamlessly leads us to our next entry...



${h2{"I've reached my home folder size / file quota"}}

OpenMOLE generates a fair amount of temporary files in the ${i{".openmole/mymachine/.tmp"}} folder associated to your machine.
Although these are deleted at the end of an execution, they can lead to a significant increase of the space occupied by your ${i{".openmole"}} folder, and of the number of files present in the same folder.
Because some systems place stringent limitations on these two quotas, you might want to move your ${i{".openmole"}} folder to a file system not restricted by quotas in order to run your OpenMOLE experiment successfully.
The simplest way to do so is to create a destination folder in the unrestricted file system and then create a symbolic link name ${i{".openmole"}} in your home directory that points to this newly created folder.
On a UNIX system, this procedure translates into the following commands:

${hl("""
# assumes /data is not restricted by quotas
cp -r ~/.openmole /data/openmole_data
rm -rf ~/.openmole
ln -s /data/openmole_data ~/.openmole
""", "plain")}

In order for this procedure to work, you'll want to ensure the target folder (${i{"/data/openmole"}} in the example) can be reached from all the machines running your OpenMOLE installation.

$br

Moving your ${i{".openmole"}} folder to a different location is also strongly advised on remote execution hosts (typically clusters) on which you own a personal account used with OpenMOLE.
In the case of remote environments, the OpenMOLE runtime and the input files of your workflow will be copied to the ${i{".openmole"}} folder, again leading to problematic over quotas on these systems.
For this specific case, we recommend using the ${code{"sharedDirectory"}} option of the ${a("cluster environment", href := cluster.file)} to set the location where OpenMOLE should copy your files without hitting any quota restrictions.



${h2{"My sampling generates a type error"}}

Combining samplings is straightforward in OpenMOLE, but can sometimes results in syntax errors a bit cryptic to new users.
Let's take the example of a combined sampling made of a file exploration sampling and an integer range exploration:

${hl.code("""
(input in (workDirectory / "../data/").files withName inputName) x
i in (1 to 10)
""")}

This combined sampling will generate the following error when compiling the workflow:

${hl("""
found   : org.openmole.core.workflow.data.Prototype[Int]
required: org.openmole.core.workflow.sampling.Sampling
""", "plain")}

OpenMOLE cannot identify the integer range as a valid sampling.
Simply wrapping the expression in parentheses fixes the problem as shown in this correct version:

${hl.code("""
(input in (workDirectory / "../data/").files withName inputName) x
(i in (1 to 10))
""")}

${h2{"I get an error related to files on Linux and there is 'too many open files' written somewhere in the error"}}

On Linux servers, the number of files a user can open is generally limited to 1024.
OpenMOLE increases this number to 4096 on launch, but if it doesn't seem to work on your system, you might want to understand why.
To check the current state of your system limit, execute ${code{"ulimit -a"}} in a terminal:

${hl("""
reuillon@docker-host1:~$ ulimit -a
core file size          (blocks, -c) 0
data seg size           (kbytes, -d) unlimited
scheduling priority             (-e) 0
file size               (blocks, -f) unlimited
pending signals                 (-i) 64040
max locked memory       (kbytes, -l) 64
max memory size         (kbytes, -m) unlimited
open files                      (-n) 1024
pipe size            (512 bytes, -p) 8
POSIX message queues     (bytes, -q) 819200
real-time priority              (-r) 0
stack size              (kbytes, -s) 8192
cpu time               (seconds, -t) unlimited
max user processes              (-u) 64040
virtual memory          (kbytes, -v) unlimited
file locks                      (-x) unlimited
""", "plain")}

In this example you can see the max number of open files is 1024.
This is generally a soft limitation that can be overridden by the user.
To do so, execute ${code{"ulimit -n 4096"}} before launching OpenMOLE in the same terminal.
You can check that your command had the expected effect using ${code{"ulimit -a"}}.
If nothing changed in the terminal output, it means that a hard limit has been set in the ${i{"limits.conf"}} file of your system.
If you have root access, you can fix it by modifying the file ${i{"/etc/security/limits.conf"}}, otherwise you should contact the system administrator and ask them kindly to modify it.


${h2{"When shall I use Path over File?"}}

OpenMOLE takes care of everything for you, from connecting to remote environments to submitting jobs and copying your files.
However, most ${a("clusters", href := cluster.file)} installations take advantage of a shared file system between the nodes.
If the file structure you're exploring is located on a shared file system, you do not need OpenMOLE to duplicate the target files, as they are already available on the compute node directly.
In case you're manipulating very large files, it might not even be possible to duplicate them.
When you find yourself in such a use case, you might want to try the ${code{"Path"}} optimization for your scripts.
By replacing the ${code{"Val[File]"}} variables by ${code{"Val[Path]"}} in your scripts, OpenMOLE will store the file's ${i{"location"}} and not its ${i{"content"}} as it would when using ${code{"Val[File]"}}.
This optimization is only available for ${a("clusters", href := cluster.file)} and ${b{"not"}} for the ${a("EGI grid", href := egi.file)}.
You can find an example of using ${code{"Path"}} variables in the dataflow in the ${a("data processing", href := fileSampling.file)} page.



${h2{"My problem is not listed here"}}

If you could not resolve your problem, feel free to post your problem on the ${a("forum", href := shared.link.forum)},or ask us directly on our ${a("chat", href := shared.link.chat)}.
If you think your problem is induced by a bug in OpenMOLE, please report the issue exhaustively on our ${a("GitHub page", href := shared.link.issue)}.

""")
