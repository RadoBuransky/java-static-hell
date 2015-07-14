package com.buransky.javaStaticHell

import org.apache.bcel.classfile.{EmptyVisitor, JavaClass, Method}
import org.apache.bcel.generic.{ConstantPoolGen, MethodGen}

import scala.collection.mutable.ListBuffer

private[javaStaticHell] class ClassVisitor(javaClass: JavaClass) extends EmptyVisitor {
  private lazy val constants = new ConstantPoolGen(javaClass.getConstantPool)
  private val buffer = new ListBuffer[String]

  def staticDependencies(): Iterable[String] = {
    visitJavaClass(javaClass)
    buffer.distinct
  }

  override def visitJavaClass(jc: JavaClass): Unit = {
    jc.getConstantPool.accept(this)
    jc.getMethods.foreach(_.accept(this))
  }

  override def visitMethod(method: Method): Unit = {
    val methodGen = new MethodGen(method, javaClass.getClassName, constants)
    val methodVisitor = new MethodVisitor(methodGen, javaClass)
    buffer ++= methodVisitor.staticDependencies()
  }
}
