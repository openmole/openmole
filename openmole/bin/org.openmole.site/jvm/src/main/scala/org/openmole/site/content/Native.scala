
package org.openmole.site.content

import org.openmole.site.tools._
import org.openmole.site.{DocumentationPages, Pages, Resource, shared}

import scalatags.Text.all.{br, _}

object Native {
  def introPackagingForRPythonCplusplus = paragraph(
    div("Most of the time, model code is not designed to be portable. OpenMOLE handles natively Java, Scala, NetLogo and R via specific Tasks. However, if no specific task has yet been designed for the language/platform you can still plug you code into OpenMOLE using:", marginTop := 30),
      ul(
        li("either the ", a("container task documentation", href := DocumentationPages.container.file, targetBlank)),
        li("or the ", a("native packaging documentation", href := DocumentationPages.container.file, targetBlank), "."),
      )
    )


//  def scriptsCommon(language: String, commandLine: String) = paragraph(
//    p, "To call this script from the command line you should type: ",
//    hl(commandLine, "plain"),
//    " considering you have ", i(language), " installed on your system.",
//
//    p, s"Once the script is up and running, remember that the ", b("first step to run it from OpenMOLE is to package it"), ". This is done using ", b("CARE"), " on your system.",
//    br, hl(s"care -r ~ -o $language.tgz.bin $commandLine", "plain"),
//
//    p, "Notice how the command line is identical to the original one. The call to the ", i(language), " script remains unchanged, as CARE and its options are inserted at the beginning of the command line.",
//
//    p, "The result of the previous command line is a file named ", i(s"$language.tgz.bin"), ". It is an archive containing a portable version of your execution. It can be extracted and executed on any other Linux platform.",
//
//    p, "The method described here ", b("packages everything"), " including ", i(language), " itself! Therefore there is ", b("no need to install "), b(i(language)), b(" on the target execution machine"), ". All that is needed is for the remote execution host to run Linux, which is the case for the vast majority of (decent) high performance computing environments.",
//
//    p, "Packaging an application is done ", b("once and for all"), " by running the original application against CARE. CARE's re-execution mechanisms allows you to change the original command line when re-running your application. This way ", b("you can update the parameters passed on the command line"), " and the re-execution will be impacted accordingly. As long as all the configuration files, libraries, ... were used during the original execution, there is ", b("no need to package the application multiple times with different input parameters"), "."
//
//  )

  val footer = paragraph(
    p, "Two things should be noted from this example:",
    ul,
    li(RawFrag(s"The procedure to package an application ${b("is always the same")},  regardless of the underlying programming language / framework used.")),
    li(RawFrag(s"The ${hl.openmoleNoTest("CARETask")} is not different from the SystemExecTask to the extent of the archive given as a first parameter.")),

    p, "These two aspects make it really ", b("easy to embed native applications"), " in OpenMOLE. You can also read more about packaging your native models for OpenMOLE in ", a("the dedicated section", href := DocumentationPages.container.file), "."
  )

  val singularity = a("the Singularity container system", href := org.openmole.site.shared.link.singularity )

  def usesSingularity(taskName: String) =
    paragraph(
      "The ",
      org.openmole.site.tools.code(taskName),
      s" uses ", singularity, ". You should install Singularity on your system otherwise you won't be able to use it."
    )

}
