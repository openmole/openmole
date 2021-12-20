package org.openmole.core.dsl

package object extension {

  type FromContext[+T] = org.openmole.core.expansion.FromContext[T]
  lazy val FromContext = org.openmole.core.expansion.FromContext

  type DefinitionScope = org.openmole.core.workflow.builder.DefinitionScope
  def DefinitionScope = org.openmole.core.workflow.builder.DefinitionScope

  type CacheKey[T] = org.openmole.tool.cache.CacheKey[T]
  def CacheKey = org.openmole.tool.cache.CacheKey

  type ScalarOrSequenceOfDouble = org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble
  def ScalarOrSequenceOfDouble = org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble

  type Hook = org.openmole.core.workflow.hook.FromContextHook
  def Hook = org.openmole.core.workflow.hook.Hook
  def FormattedFileHook = org.openmole.core.workflow.hook.FormattedFileHook

  type Source = org.openmole.core.workflow.mole.Source
  type FromContextSource = org.openmole.core.workflow.mole.FromContextSource
  def Source = org.openmole.core.workflow.mole.Source

  type Task = org.openmole.core.workflow.task.Task
  type FromContextTask = org.openmole.core.workflow.task.FromContextTask
  def Task = org.openmole.core.workflow.task.FromContextTask

  type Grouping = org.openmole.core.workflow.grouping.Grouping

  type Sampling = org.openmole.core.workflow.sampling.Sampling
  type FromContextSampling = org.openmole.core.workflow.sampling.FromContextSampling
  def Sampling = org.openmole.core.workflow.sampling.Sampling
  type IsSampling[-T] = org.openmole.core.workflow.sampling.IsSampling[T]

  type BoundedFromContextDomain[-D, +T] = org.openmole.core.workflow.domain.BoundedFromContextDomain[D, T]
  type BoundedDomain[-D, +T] = org.openmole.core.workflow.domain.BoundedDomain[D, T]
  type FixDomain[-D, +T] = org.openmole.core.workflow.domain.FixDomain[D, T]
  type DiscreteDomain[-D, +T] = org.openmole.core.workflow.domain.DiscreteDomain[D, T]
  type DiscreteFromContextDomain[-D, +T] = org.openmole.core.workflow.domain.DiscreteFromContextDomain[D, T]

  type DomainCenter[-D, +T] = org.openmole.core.workflow.domain.DomainCenter[D, T]
  type DomainCenterFromContext[-D, +T] = org.openmole.core.workflow.domain.DomainCenterFromContext[D, T]
  type DomainSize[-D] = org.openmole.core.workflow.domain.DomainSize[D]

  type Domain[+D] = org.openmole.core.workflow.domain.Domain[D]
  def Domain = org.openmole.core.workflow.domain.Domain

  type Factor[D, T] = org.openmole.core.workflow.sampling.Factor[D, T]
  def Factor[D, T](p: Val[T], d: D) = org.openmole.core.workflow.sampling.Factor(p, d)

  type Environment = org.openmole.core.workflow.execution.Environment
  def Environment = org.openmole.core.workflow.execution.Environment

  type Namespace = org.openmole.core.context.Namespace
  def Namespace = org.openmole.core.context.Namespace

  type Variable[T] = org.openmole.core.context.Variable[T]
  def Variable = org.openmole.core.context.Variable

  type ValType[T] = org.openmole.core.context.ValType[T]
  def ValType = org.openmole.core.context.ValType

  type Context = org.openmole.core.context.Context
  def Context = org.openmole.core.context.Context

  type WritableOutput = org.openmole.core.workflow.format.WritableOutput
  val WritableOutput = org.openmole.core.workflow.format.WritableOutput

  val OutputFormat = org.openmole.core.workflow.format.OutputFormat
  type OutputFormat[T, D] = org.openmole.core.workflow.format.OutputFormat[T, D]
  type OutputContent = OutputFormat.OutputContent
  type HookExecutionContext = org.openmole.core.workflow.hook.HookExecutionContext

  def ExpandedString = org.openmole.core.expansion.ExpandedString

  type Negative[+A] = org.openmole.core.keyword.Negative[A]
  type Under[+A, +B] = org.openmole.core.keyword.Under[A, B]
  type In[+A, +B] = org.openmole.core.keyword.In[A, B]
  type :=[+A, +B] = org.openmole.core.keyword.:=[A, B]
  type Aggregate[+A, B] = org.openmole.core.keyword.Aggregate[A, B]
  type Delta[+A, +B] = org.openmole.core.keyword.Delta[A, B]
  type As[+A, +B] = org.openmole.core.keyword.As[A, B]
  type On[+A, +B] = org.openmole.core.keyword.On[A, B]
  type By[+A, +B] = org.openmole.core.keyword.By[A, B]

  type ValueAssignment[T] = org.openmole.core.workflow.builder.ValueAssignment[T]

  def On = org.openmole.core.keyword.On
  def By = org.openmole.core.keyword.By
  def Aggregate = org.openmole.core.keyword.Aggregate

  type TmpDirectory = org.openmole.core.workspace.TmpDirectory
  def TmpDirectory = org.openmole.core.workspace.TmpDirectory

  type FileService = org.openmole.core.fileservice.FileService

  type Validate = org.openmole.core.expansion.Validate
  def Validate = org.openmole.core.expansion.Validate

  type UserBadDataError = org.openmole.core.exception.UserBadDataError
  type RandomProvider = org.openmole.tool.random.RandomProvider
  type PrototypeSet = org.openmole.core.context.PrototypeSet

  type Time = squants.Time
  type Information = squants.information.Information

  type JavaLogger = org.openmole.tool.logger.JavaLogger
  def Logger = org.openmole.tool.logger.LoggerService
}
