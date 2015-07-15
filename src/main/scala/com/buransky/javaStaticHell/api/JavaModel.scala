package com.buransky.javaStaticHell.api

trait Id {
  def value: String
}

trait Package {
  def parent: Option[Package]
  def name: Id
}

object Package {
  def apply(name: String): Package = ???
}

trait Class {
  def pckg: Package
  def name: Id
}

trait InnerClass extends Class {
  def parent: Class
}

trait Method {
  def clazz: Class
  def name: Id
  def methodType: MethodType
}

trait MethodInvocation {
  def from: Method
  def to: Method
}

sealed trait MethodType
case object StaticMethodType extends MethodType
case object VirtualMethodType extends MethodType