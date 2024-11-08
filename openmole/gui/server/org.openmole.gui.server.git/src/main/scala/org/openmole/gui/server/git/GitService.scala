package org.openmole.gui.server.git

import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.{CheckoutConflictException, StashApplyFailureException, TransportException}
import org.eclipse.jgit.storage.file.*
import org.openmole.gui.shared.data.*

import java.io.File
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.config.hosts.{HostConfigEntry, HostConfigEntryResolver}
import org.apache.sshd.common.AttributeRepository
import org.apache.sshd.common.file.FileSystemFactory
import org.apache.sshd.common.session.SessionContext
import org.apache.sshd.common.signature.BuiltinSignatures
import org.apache.sshd.git.transport.GitSshdSessionFactory
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.transport.{CredentialItem, CredentialsProvider, SshSessionFactory, SshTransport, URIish}
import org.openmole.gui.shared.data

import java.net.SocketAddress
import java.nio.file.{FileSystem, Path}
import java.security.Provider
import org.openmole.tool.file.*

object GitService:

  case class Factory(factory: SshSessionFactory, client: SshClient):
    def close() = client.close()

  def git(file: File, ceilingDirectory: File): Option[Git] =
    val builder = (new FileRepositoryBuilder).readEnvironment().addCeilingDirectory(ceilingDirectory).findGitDir(file)

    builder.getGitDir match
      case null => None
      case _ => Some(new Git(builder.build()))

  def withGit[T](fromFile: File, ceilingDir: File)(g: Git => T) =
    GitService.git(fromFile, ceilingDir).map: git =>
      try g(git)
      finally git.close()

  def rootPath(git: Git) = git.getRepository.getDirectory.getParentFile.getAbsolutePath

  def relativeName(f: File, git: Git) = (f.getAbsolutePath.split("/") diff rootPath(git).split("/")).mkString("/")

  def clone(remoteURL: String, destination: File, authentication: Seq[GitAuthentication.PrivateKey]) =
    destination.getParentFile.mkdirs()
    withConfiguredTransport(authentication, Git.cloneRepository):
      _.setURI(remoteURL).setDirectory(destination).call

  def commit(files: Seq[File], message: String)(implicit git: Git) =
    @tailrec def addCommitedFiles(fs: Seq[File], commitCommand: CommitCommand): CommitCommand =
      if fs.isEmpty
      then commitCommand
      else addCommitedFiles(fs.tail, commitCommand.setOnly(relativeName(fs.head, git)))

    addCommitedFiles(files, git.commit).setMessage(message).call

  def revert(files: Seq[File])(implicit git: Git) =

    @tailrec def addPath0(fs: Seq[File], checkoutCommand: CheckoutCommand): CheckoutCommand =
      if fs.isEmpty
      then checkoutCommand
      else addPath0(fs.tail, checkoutCommand.addPath(relativeName(fs.head, git)))

    addPath0(files, git.checkout).call
    ()

  def add(files: Seq[File])(implicit git: Git) =

    @tailrec def addPath0(fs: Seq[File], addCommand: AddCommand): AddCommand =
      if fs.isEmpty
      then addCommand
      else addPath0(fs.tail, addCommand.addFilepattern(relativeName(fs.head, git)))

    addPath0(files, git.add).call

  def pull(git: Git, authentication: Seq[GitAuthentication.PrivateKey]) =
    if !git.stashList.call.isEmpty
    then
      try
        withConfiguredTransport(authentication, git.pull)(_.call)
        MergeStatus.Ok
      catch case e: CheckoutConflictException => MergeStatus.ChangeToBeResolved
    else MergeStatus.Empty

  def push(git: Git, authentication: Seq[GitAuthentication.PrivateKey]): PushStatus =
    try
      withConfiguredTransport(authentication, git.push)(_.call)
      PushStatus.Ok
    catch
      case e: TransportException => PushStatus.AuthenticationRequired

  def branchList(implicit git: Git): Seq[String] =
    git.branchList.setListMode(ListMode.ALL).call().asScala.toSeq.map(_.getName).sorted

  def checkout(branchName: String)(implicit git: Git) =
    if git.branchList.call().asScala.toSeq.map(_.getName).exists(_.contains(branchName))
    then git.checkout.setName(branchName).call
    else
      git
        .branchList.setListMode(ListMode.REMOTE).call.asScala.toSeq
        .filter(b => b.getName.contains(branchName)).headOption
        .foreach: ref =>
          val remote = ref.getName.split("/")(2)
          git
            .checkout
            .setCreateBranch(true)
            .setName(branchName)
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint(s"$remote/$branchName")
            .call

  def stash(implicit git: Git): Unit =
    git.stashCreate.call

  def stashPop(implicit git: Git): MergeStatus =
    if !git.stashList.call.isEmpty
    then
      try
        git.stashApply.call
        MergeStatus.Ok
      catch case e: StashApplyFailureException => MergeStatus.ChangeToBeResolved
    else MergeStatus.Empty

  private def getAllSubPaths(path: String) =
    val allDirs = path.split("/").dropRight(1)
    val size = allDirs.size
    (for i <- 1 to size yield allDirs.dropRight(size - i).mkString("/")) :+ path

  def getModified(git: Git): Seq[String] =
    val status = git.status().call()
    (status.getModified.asScala.toSeq ++ status.getAdded.asScala.toSeq).flatMap(getAllSubPaths)

  def getUntracked(git: Git): Seq[String] = git.status().call().getUntracked.asScala.toSeq.flatMap(getAllSubPaths)

  def getConflicting(git: Git): Seq[String] = git.status().call().getConflicting.asScala.toSeq.flatMap(getAllSubPaths)

  def withConfiguredTransport[T <: TransportCommand[?, ?], R](authentication: Seq[GitAuthentication.PrivateKey], transportCommand: T)(f: T => R) =
    val prov = new CredentialsProvider:
      override def isInteractive: Boolean = false

      override def supports(items: CredentialItem*): Boolean = true

      override def get(uri: URIish, items: CredentialItem*): Boolean = true

    val factory = sshdSessionFactory(authentication)
    try
      transportCommand.setCredentialsProvider(prov)
      transportCommand.setTransportConfigCallback:
        case t: SshTransport =>
          t.setSshSessionFactory(factory.factory)
        case _ =>
      f(transportCommand)
    finally factory.close()


  def sshdSessionFactory(authentications: Seq[GitAuthentication.PrivateKey]): Factory =
    import org.apache.sshd.common.util.security.*
    import org.apache.sshd.common.config.keys.*
    import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier
    import scala.jdk.CollectionConverters.*
    import org.apache.sshd.client.*

    val sshClient: SshClient = ClientBuilder.builder().build()

    sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE)

    sshClient.setSignatureFactories(
      List(
        BuiltinSignatures.ed25519,
        BuiltinSignatures.ed25519_cert,
        BuiltinSignatures.sk_ssh_ed25519).asJava)

    val loader = SecurityUtils.getKeyPairResourceParser()

    val keys =
      authentications.flatMap: git =>
        loader.loadKeyPairs(null, git.privateKey.toPath, FilePasswordProvider.of(git.password)).asScala

    keys.foreach(sshClient.addPublicKeyIdentity)
    sshClient.setHostConfigEntryResolver:
      (host: String, port: Int, socketAddress: SocketAddress, username: String, jump: String, attributeRepository: AttributeRepository) =>
        val portValue = if port < 0 then 22 else port
        HostConfigEntry(host, host, portValue, username, jump)

    val sshSessionFactory = new GitSshdSessionFactory(sshClient)

    Factory(sshSessionFactory, sshClient)

