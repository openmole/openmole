
package org.openmole.site.content

import org.openmole.site.tools._
import org.openmole.site.{ DocumentationPages, Pages, Resource, shared }

import scalatags.Text.all._

object Native {

  def preamble = paragraph("In OpenMOLE, a generic task named ", hl.highlight("CARETask", "plain"), " offers to run external applications packaged with ", a("CARE", href := Resource.script.care.file),
    ". The site (proposing an outdated version of CARE for now, but a great documentation) can be found ", a("here", href := shared.link.CAREsite), ". ",
    "CARE makes it possible to package your application from any Linux computer, and then re-execute it on any other Linux computer. The CARE / OpenMOLE pair is a very efficient way to distribute your application at very large scale with very little effort. Please note that this packaging step is only necessary if you plan distribute your workflow to an heterogeneous computing environment such as the EGI grid. If you target local clusters, running the same operating system and sharing a network file system, you can directly jump to the ", a("SystemExecTask.", href := DocumentationPages.nativePackaging.file + "#Usinglocalexecutable"))

  def installCARE = paragraph(p("You should first install CARE:"), listInstallCARE)

  def listInstallCARE = htmlList(
    listItem("download the CARE tool from ", a("here", href := Resource.script.care.file)),
    listItem("make it executable (", hl.highlight("chmod +x care", "plain"), ")"),
    listItem("add the path to the executable to your PATH variable (", hl.highlight("export PATH=/path/to/the/care/folder:$PATH", "plain"), ")")
  )

  def paragraphEmbed = paragraph(p("The ", hl.openmole("CARETask"), " was designed to embed native binaries such as programs compiled from C, C++, Fortran, Python, R, Scilab... Embedding an application in a ", hl.openmole("CARETask"), a(" happens in 2 steps:")))

  def firstStep = paragraph(p(b("First"), " you should package your application using the CARE binary you just installed, so that it executes on any Linux environment. This usually consists in prepending your command line with: ", br, hl.highlight("care -o /path/to/myarchive.tgz.bin -r ~ -p /path/to/mydata1 -p /path/to/mydata2 mycommand myparam1 myparam2", "plain"), br, "Before going any further, here are a few notes about the options accepted by CARE:"))

  def listOptionsCARE = htmlList(
    listItem(hl.highlight("-o", "plain"), " indicates where to store the archive. At the moment, OpenMOLE prefers to work with archives stored in ", i(".tgz.bin"), " so please don't toy with the extension ;-)"),
    listItem(hl.highlight("-r ~", "plain"), " is not compulsory but it has proved mandatory in some cases. So as rule of thumb, if you encounter problems when packaging your application, try adding / removing it."),
    listItem(hl.highlight("-p /path", "plain"), " asks CARE not to archive ", i("/path"), ". This is particularly useful for input data that will change with your parameters. ", b("You probably do not want"), " to embed this data in the archive, and we'll see further down how to inject the necessary input data in the archive from OpenMOLE.")
  )

  def secondStep = paragraph(p(b("Second"), ", just provide the resulting package along with some other information to OpenMOLE. Et voila! If you encounter any problem to package your application, please refer to the corresponding entry in the ", a("FAQ", href := Pages.faq.file + "#Ican'tgetCARE/PRoottowork")))

  def importantAspectCARE = paragraph(p("One very important aspect of CARE is that ", b("you only need to package your application once"), ". As long as the execution you use to package your application makes uses of all the dependencies (libraries, packages, ...), you should not have any problem re-executing this archive with other parameters."))

  def introPackagingForRPythonCplusplus = paragraph(
    "Most of the time, model code is not designed to be portable. For now, OpenMOLE handles Java, Scala, NetLogo and R (in the near future) via specific Tasks, but it is still far from covering the languages used to develop models around the world.",
    p("Meanwhile, you have to package your code using CARE, as explained in the ", a("Native Packaging section", href := DocumentationPages.nativePackaging.file, targetBlank), ". The following contents expose how to handle your packaged model within OpenMOLE.")
  )

  def scriptsCommon(language: String, commandLine: String) = paragraph(
    p, "To call this script from the command line you should type: ",
    hl.highlight(commandLine, "plain"),
    " considering you have ", i(language), " installed on your system.",

    p, s"Once the script is up and running, remember that the ", b("first step to run it from OpenMOLE is to package it"), ". This is done using ", b("CARE"), " on your system.",
    br, hl.highlight(s"care -r ~ -o $language.tgz.bin $commandLine", "plain"),

    p, "Notice how the command line is identical to the original one. The call to the ", i(language), " script remains unchanged, as CARE and its options are inserted at the beginning of the command line.",

    p, "The result of the previous command line is a file named ", i(s"$language.tgz.bin"), ". It is an archive containing a portable version of your execution. It can be extracted and executed on any other Linux platform.",

    p, "The method described here ", b("packages everything"), " including ", i(language), " itself! Therefore there is ", b("no need to install "), b(i(language)), b(" on the target execution machine"), ". All that is needed is for the remote execution host to run Linux, which is the case for the vast majority of (decent) high performance computing environments.",

    p, "Packaging an application is done ", b("once and for all"), " by running the original application against CARE. CARE's re-execution mechanisms allows you to change the original command line when re-running your application. This way ", b("you can update the parameters passed on the command line"), " and the re-execution will be impacted accordingly. As long as all the configuration files, libraries, ... were used during the original execution, there is ", b("no need to package the application multiple times with different input parameters"), "."

  )

  val footer = paragraph(
    p, "Two things should be noted from this example:",
    ul,
    li(RawFrag(s"The procedure to package an application ${b("is always the same")},  regardless of the underlying programming language / framework used.")),
    li(RawFrag(s"The ${hl.openmoleNoTest("CARETask")} is not different from the ${a("SystemExecTask", href := DocumentationPages.nativePackaging.anchor(shared.nativePackagingMenu.localExecutable))} to the extent of the archive given as a first parameter.")),

    p, "These two aspects make it really ", b("easy to embed native applications"), " in OpenMOLE. You can also read more about packaging your native models for OpenMOLE in ", a("the dedicated section", href := DocumentationPages.nativePackaging.file), "."
  )

}
