package org.openmole.core.dsl

package object extension {
  type FromContext[T] = org.openmole.core.expansion.FromContext[T]
  lazy val FromContext = org.openmole.core.expansion.FromContext

  type DefinitionScope = org.openmole.core.workflow.builder.DefinitionScope
  def DefinitionScope = org.openmole.core.workflow.builder.DefinitionScope

  type CacheKey[T] = org.openmole.tool.cache.CacheKey[T]
  def CacheKey = org.openmole.tool.cache.CacheKey

  type ScalarOrSequenceOfDouble[T] = org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble[T]
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

  type Sampling = org.openmole.core.workflow.sampling.Sampling
  type FromContextSampling = org.openmole.core.workflow.sampling.FromContextSampling
  def Sampling = org.openmole.core.workflow.sampling.Sampling

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

  implicit def validationOfFromContext(f: FromContext[_]) =
    (p: FromContext.ValidationParameters) ⇒ f.validate(p.inputs)(p.newFile, p.fileService)

  type Negative[A] = org.openmole.core.keyword.Negative[A]
  type Under[A, B] = org.openmole.core.keyword.Under[A, B]
  type In[A, B] = org.openmole.core.keyword.In[A, B]
  type :=[A, B] = org.openmole.core.keyword.:=[A, B]
  type Aggregate[A, B] = org.openmole.core.keyword.Aggregate[A, B]
  type Delta[A, B] = org.openmole.core.keyword.Delta[A, B]
  type As[A, B] = org.openmole.core.keyword.As[A, B]

  def Aggregate = org.openmole.core.keyword.Aggregate

}
