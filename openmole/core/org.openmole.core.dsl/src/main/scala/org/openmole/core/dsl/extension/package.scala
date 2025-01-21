package org.openmole.core.dsl

package object extension:
  export org.openmole.core.argument.{FromContext, ScalaCompilation, ToFromContext, OptionalArgument, DefaultSet}
  
  type DefinitionScope = org.openmole.core.setter.DefinitionScope
  def DefinitionScope = org.openmole.core.setter.DefinitionScope

  type CacheKey[T] = org.openmole.tool.cache.CacheKey[T]
  def CacheKey = org.openmole.tool.cache.CacheKey

  export org.openmole.core.workflow.sampling.ScalableValue

  type Hook = org.openmole.core.workflow.hook.FromContextHook
  def Hook = org.openmole.core.workflow.hook.Hook
  def FormattedFileHook = org.openmole.core.workflow.hook.OMRFileHook

  type Source = org.openmole.core.workflow.mole.Source
  type FromContextSource = org.openmole.core.workflow.mole.FromContextSource
  def Source = org.openmole.core.workflow.mole.Source

  type FromContextTask = org.openmole.core.workflow.task.FromContextTask
  export org.openmole.core.workflow.task.Task

  export org.openmole.core.workflow.task.TaskExecution
  export org.openmole.core.workflow.task.TaskExecutionContext
  export org.openmole.core.workflow.task.TaskExecutionBuildContext


  type Grouping = org.openmole.core.workflow.grouping.Grouping

  type Sampling = org.openmole.core.workflow.sampling.Sampling
  type FromContextSampling = org.openmole.core.workflow.sampling.FromContextSampling
  def Sampling = org.openmole.core.workflow.sampling.Sampling
  type IsSampling[-T] = org.openmole.core.workflow.sampling.IsSampling[T]

  export org.openmole.core.workflow.domain.BoundedFromContextDomain
  export org.openmole.core.workflow.domain.BoundedDomain
  export org.openmole.core.workflow.domain.FixDomain
  export org.openmole.core.workflow.domain.DiscreteDomain
  export org.openmole.core.workflow.domain.DiscreteFromContextDomain

  export org.openmole.core.workflow.domain.DomainCenter
  export org.openmole.core.workflow.domain.DomainCenterFromContext
  export org.openmole.core.workflow.domain.DomainSize
  export org.openmole.core.workflow.domain.DomainStep

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

  export org.openmole.core.context.Context
  export org.openmole.core.format.WritableOutput
  export org.openmole.core.format.WritableOutput.Display
  export org.openmole.core.format.{MethodMetaData, ValData, omrCirceDefault}
  export org.openmole.core.format.OutputFormat.{OutputContent, SectionContent}
  export org.openmole.core.format.OutputFormat

  type HookExecutionContext = org.openmole.core.workflow.hook.HookExecutionContext

  def ExpandedString = org.openmole.core.argument.ExpandedString

  type Negative[+A] = org.openmole.core.keyword.Negative[A]
  type Under[+A, +B] = org.openmole.core.keyword.Under[A, B]
  type In[+A, +B] = org.openmole.core.keyword.In[A, B]
  type :=[+A, +B] = org.openmole.core.keyword.:=[A, B]
  type Evaluate[+A, B] = org.openmole.core.keyword.Evaluate[A, B]
  type Delta[+A, +B] = org.openmole.core.keyword.Delta[A, B]
  type As[+A, +B] = org.openmole.core.keyword.As[A, B]
  type On[+A, +B] = org.openmole.core.keyword.On[A, B]
  type By[+A, +B] = org.openmole.core.keyword.By[A, B]

  export org.openmole.core.setter.{
      ValueAssignment,
      InputOutputConfig,
      InfoConfig,
      MappedInputOutputConfig,
      Mapped,
      InputOutputBuilder,
      InfoBuilder,
      MappedInputOutputBuilder,
      Setter
    }

  def On = org.openmole.core.keyword.On
  def By = org.openmole.core.keyword.By
  def Aggregate = org.openmole.core.keyword.Evaluate


  type Validate = org.openmole.core.argument.Validate
  def Validate = org.openmole.core.argument.Validate

  export org.openmole.core.exception.{UserBadDataError, InternalProcessingError}

  type RandomProvider = org.openmole.tool.random.RandomProvider
  type PrototypeSet = org.openmole.core.context.PrototypeSet

  type Time = squants.Time
  type Information = squants.information.Information

  type JavaLogger = org.openmole.tool.logger.JavaLogger
  def Logger = org.openmole.tool.logger.LoggerService

  export org.openmole.tool.types.TypeTool.ManifestDecoration
  export org.openmole.tool.crypto.Cypher

  export org.openmole.core.replication.ReplicaCatalog
  export org.openmole.core.workflow.execution.EnvironmentBuilder
  export org.openmole.core.timeservice.TimeService
  export org.openmole.core.pluginmanager.PluginManager
  export org.openmole.core.workflow.validation.ValidateTask

  export org.openmole.core.preference.Preference
  export org.openmole.core.threadprovider.ThreadProvider
  export org.openmole.core.workspace.{TmpDirectory, Workspace}
  export org.openmole.core.fileservice.FileService
  export org.openmole.core.outputmanager.OutputManager
  export org.openmole.tool.outputredirection.OutputRedirection
  export org.openmole.core.networkservice.NetworkService
  export org.openmole.core.serializer.SerializerService

  export org.openmole.core.highlight.HighLight
  export org.openmole.core.pluginregistry.PluginRegistry
  
  export org.openmole.core.workflow.mole.MoleExecution
  export org.openmole.core.services.Services

  export org.openmole.tool.cache.KeyValueCache
  export org.openmole.tool.collection.DoubleRange

  export org.openmole.core.setter.DefaultBuilder
  export org.openmole.core.script.ScriptSourceData
  export org.openmole.core.authentication.AuthenticationStore




