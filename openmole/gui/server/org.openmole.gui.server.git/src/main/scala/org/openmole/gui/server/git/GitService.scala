package org.openmole.gui.server.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.*
import org.openmole.gui.shared.data.{GitStatus, SafePath}
import java.io.File
import scala.jdk.CollectionConverters.*

object GitService:

  def git(file: File, ceilingDirectory: File): Option[Git] =
    println("F " + file.getAbsolutePath + " / " + file.exists + " / " + ceilingDirectory.getAbsolutePath)
    val builder = (new FileRepositoryBuilder)
      .addCeilingDirectory(ceilingDirectory)
    
    builder.getGitDir match
      case null => None
      case _ => Some(new Git(builder.build()))

  def clone(remoteURL: String, destination: File) =
    val dest = new File(destination, remoteURL.split("/").last)
    println("DEST " + dest.getAbsolutePath)
    Git.cloneRepository()
      .setURI(remoteURL)
      .setDirectory(dest)
      .call()
      ()

  def getModified(git: Git): Seq[String] = git.status().call().getModified.asScala.toSeq

  def getUntracked(git: Git): Seq[String] = git.status().call().getUntracked.asScala.toSeq

  def getConflicting(git: Git): Seq[String] = git.status().call().getConflicting.asScala.toSeq


