package com.buransky.javaStaticHell.api

import java.nio.file.Path

/**
 * Main API.
 */
trait JavaStaticHellApi {
  def methodInvocations: MethodInvocationApi
}

trait MethodInvocationApi {
  def fromJar(jarPath: Path): MethodInvocationsFromJarApi
}

trait MethodInvocationsFromJarApi {
  def all: MethodInvocationsApi
  def static: MethodInvocationsApi
}

trait MethodInvocationsApi {
  def exclude(pckg: Package): MethodInvocationsApi
  def exclude(pckg: MethodType): MethodInvocationsApi
  def get: Traversable[MethodInvocation]
}