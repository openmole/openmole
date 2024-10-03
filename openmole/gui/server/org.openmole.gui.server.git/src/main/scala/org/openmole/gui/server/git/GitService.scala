package org.openmole.gui.server.git

import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.{CheckoutConflictException, StashApplyFailureException}
import org.eclipse.jgit.storage.file.*
import org.openmole.gui.shared.data.*

import java.io.File
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

object GitService:

  def git(file: File, ceilingDirectory: File): Option[Git] =
    val builder = (new FileRepositoryBuilder).readEnvironment().addCeilingDirectory(ceilingDirectory).findGitDir(file)
    
    builder.getGitDir match
      case null => None
      case _ => Some(new Git(builder.build()))


  def withGit(fromFile: File, ceilingDir: File)(g: Git=> Unit) =
      GitService.git(fromFile, ceilingDir) match
        case Some(git: Git)=> g(git)
        case _=>

  def rootPath(git: Git) = git.getRepository.getDirectory.getParentFile.getAbsolutePath
  
  def relativeName(f: File, git: Git) = (f.getAbsolutePath.split("/") diff rootPath(git).split("/")).mkString("/")
  
  def clone(remoteURL: String, destination: File) =
    val dest = new File(destination, remoteURL.split("/").last)
    Git.cloneRepository
      .setURI(remoteURL)
      .setDirectory(dest)
      .call

  def commit(files: Seq[File], message: String)(implicit git: Git) =

    @tailrec
    def addCommitedFiles(fs: Seq[File], commitCommand: CommitCommand): CommitCommand =
    if fs.isEmpty
    then commitCommand
    else addCommitedFiles(fs.tail, commitCommand.setOnly(relativeName(fs.head, git)))
        
    addCommitedFiles(files, git.commit).setMessage(message).call

  def revert(files: Seq[File])(implicit git: Git) =

    @tailrec
    def addPath0(fs: Seq[File], checkoutCommand: CheckoutCommand): CheckoutCommand =
      if fs.isEmpty
      then checkoutCommand
      else addPath0(fs.tail, checkoutCommand.addPath(relativeName(fs.head, git)))

    addPath0(files, git.checkout).call
    ()

  def add(files: Seq[File])(implicit git: Git) =

    @tailrec
    def addPath0(fs: Seq[File], addCommand: AddCommand): AddCommand =
      if fs.isEmpty
      then addCommand
      else addPath0(fs.tail, addCommand.addFilepattern(relativeName(fs.head, git)))

    addPath0(files, git.add).call

  def pull(implicit git: Git) =
    if !git.stashList.call.isEmpty
    then 
      try 
        git.pull.call
        MergeStatus.Ok
      catch case e: CheckoutConflictException=> MergeStatus.ChangeToBeResolved
    else MergeStatus.Empty

  def branchList(implicit git: Git): Seq[String] =
    git.branchList.call().asScala.toSeq.map(_.getName)

  def checkout(branchName: String)(implicit git: Git) =
    git.checkout.setName(branchName).call
    
  def stash(implicit git: Git): Unit =
    git.stashCreate.call
    
  def stashPop(implicit git: Git): MergeStatus =
    if !git.stashList.call.isEmpty
    then 
      try 
        git.stashApply.call
        MergeStatus.Ok
      catch case e: StashApplyFailureException=> MergeStatus.ChangeToBeResolved
    else MergeStatus.Empty
    
  private def getAllSubPaths(path: String) =
     val allDirs = path.split("/").dropRight(1)
     val size = allDirs.size
     (for i <- 1 to size
     yield allDirs.dropRight(size - i).mkString("/")) :+ path

  def getModified(git: Git): Seq[String] = git.status().call().getModified.asScala.toSeq.flatMap(getAllSubPaths(_))

  def getUntracked(git: Git): Seq[String] = git.status().call().getUntracked.asScala.toSeq.flatMap(getAllSubPaths(_))

  def getConflicting(git: Git): Seq[String] = git.status().call().getConflicting.asScala.toSeq.flatMap(getAllSubPaths(_))


