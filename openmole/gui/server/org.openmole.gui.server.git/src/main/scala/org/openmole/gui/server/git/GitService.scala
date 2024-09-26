package org.openmole.gui.server.git

import org.eclipse.jgit.api.{CommitCommand, Git, GitCommand}
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.*
import org.openmole.gui.shared.data.{GitStatus, SafePath}

import java.io.File
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

object GitService:

  def git(file: File, ceilingDirectory: File): Option[Git] =
    val builder = (new FileRepositoryBuilder).readEnvironment().addCeilingDirectory(ceilingDirectory).findGitDir(file)
    
    builder.getGitDir match
      case null => None
      case _ => Some(new Git(builder.build()))

  def rootPath(git: Git) = git.getRepository.getDirectory.getParentFile.getAbsolutePath
  
  def relativeName(f: File, git: Git) = (f.getAbsolutePath.split("/") diff rootPath(git).split("/")).mkString("/")
  
  def clone(remoteURL: String, destination: File) =
    val dest = new File(destination, remoteURL.split("/").last)
    Git.cloneRepository()
      .setURI(remoteURL)
      .setDirectory(dest)
      .call()
      ()

  def commit(files: Seq[File], message: String)(implicit git: Git) =
    
    @tailrec
    def addCommitedFiles(fs: Seq[File], commitCommand: CommitCommand): CommitCommand =
    if fs.isEmpty
    then commitCommand
    else
        addCommitedFiles(
          fs.tail,
          commitCommand.setOnly(relativeName(fs.head, git))
        )
        
    addCommitedFiles(files, git.commit()).setMessage(message).call()
    ()

  def getAllSubPaths(path: String) =
     val allDirs = path.split("/").dropRight(1)
     val size = allDirs.size
     (for i <- 1 to size
     yield allDirs.dropRight(size - i).mkString("/")) :+ path

  def getModified(git: Git): Seq[String] = git.status().call().getModified.asScala.toSeq.flatMap(getAllSubPaths(_))

  def getUntracked(git: Git): Seq[String] = git.status().call().getUntracked.asScala.toSeq.flatMap(getAllSubPaths(_))

  def getConflicting(git: Git): Seq[String] = git.status().call().getConflicting.asScala.toSeq.flatMap(getAllSubPaths(_))


