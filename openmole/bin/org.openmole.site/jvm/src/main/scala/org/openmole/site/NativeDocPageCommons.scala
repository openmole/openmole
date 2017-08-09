
package org.openmole.site

import org.openmole.site.tools._

import scalatags.Text.all._

object NativeDocPageCommons {

  def listItem(content: Frag*): Frag = li(content)
  def htmlList(items: Frag*): Frag = ul(items)

  def paragraph(body: Frag*): Frag = Seq[Frag](body)

  def preamble = paragraph("In OpenMOLE, a generic task named ", hl.highlight("CARETask", "plain"), "offers to run external applications packaged with ", a("CARE", href := Resource.script.care.file),
    ". The site (proposing an outdated version of CARE for now, but a great documentation) can be found ", a("here", href := shared.link.CAREsite), ".",
    "CARE makes it possible to package your application from any Linux computer, and then re-execute it on any other Linux computer. The CARE / OpenMOLE pair is a very efficient way to distribute your application at very large scale with very little effort. Please note that this packaging step is only necessary if you plan distribute your workflow to an heterogeneous computing environment such as the EGI grid. If you target local clusters, running the same operating system and sharing a network file system, you can directly jump to the ", a("SystemExecTask", href := DocumentationPages.native.file + "#Usingalocalexecutable(innonportabletasks)"), ", section. TODO lien mort Native Usinglocal executable")

  def installCARE = paragraph(p("You should first install CARE:"), listInstallCARE)

  def listInstallCARE = htmlList(
    listItem("download the CARE binary from ", a("here", href := Resource.script.care.file)),
    listItem("make it executable (", hl.highlight("chmod +x care", "plain"), ")"),
    listItem("add the path to the executable to your PATH variable (", hl.highlight("export PATH=/path/to/the/care/folder:$PATH", "plain"))
  )

  def paragraphEmbed = paragraph(p("The ", hl.openmole("CARETask"), " has been designed to embed native binaries such as programs compiled from C, C++, Fortran, Python, R, Scilab... Embedding an application in a ", hl.openmole("CARETask"), "happens in 2 steps:"))

  def firstStep = paragraph(p(b("First"), " you should package your application using the CARE binary you just installed, so that it executes on any Linux environment. This usually consists in prepending your command line with ", hl.highlight("care -o /path/to/myarchive.tgz.bin -r ~ -p /path/to/mydata1 -p /path/to/mydata2 mycommand myparam1 myparam2", "plain"), ". Before going any further, here are a few notes about the options passed to CARE:"))

  def listOptionsCARE = htmlList(
    listItem(hl.highlight("-o", "plain"), "indicates where to store the archive. At the moment, OpenMOLE prefers to work with archives stored in .tgz.bin, so please don't toy with the extension ;-)"),
    listItem(hl.highlight("-r ~", "plain"), "is not compulsory but it has proved mandatory in some cases. So as rule of thumb, if you encounter problems when packaging your application, try adding / removing it."),
    listItem(hl.highlight("-p /path", "plain"), " asks CARE not to archive /path. This is particularly useful for input data that will change with your parameters. ", b("You probably do not want"), " to embed this data in the archive, and we'll see further down how to inject the necessary input data in the archive from OpenMOLE.")
  )

  def secondStep = paragraph(p(b("Second"), ", just provide the resulting package along with some other information to OpenMOLE. Et voila! If you encounter any problem to package your application, please refer to the corresponding entry in the ", a("FAQ", href := Pages.faq.file + "#Ican'tgetCARE/PRoottowork")))

  def importantAspectCARE = paragraph(p("One very important aspect of CARE is that you only need to package your application @b{once}. As long as the execution you use to package your application makes uses of all the dependencies, you should not have any problem re-executing this archive with other parameters."))

}
