package com.buransky.javaStaticHell

import org.apache.bcel.classfile.{ConstantPool, EmptyVisitor, JavaClass, Method}
import org.apache.bcel.generic.{ConstantPoolGen, MethodGen}

private[javaStaticHell] class ClassVisitor(mainJavaClass: JavaClass) extends EmptyVisitor {
  private lazy val constants = new ConstantPoolGen(mainJavaClass.getConstantPool)

  def start(): Unit = visitJavaClass(mainJavaClass)

  override def visitJavaClass(jc: JavaClass): Unit = {
    jc.getConstantPool.accept(this)
    jc.getMethods.foreach(_.accept(this))
  }

  override def visitConstantPool(constantPool: ConstantPool): Unit = {
    constantPool.getConstantPool.foreach {
      case constant if constant != null && constant.getTag == 7 =>
        val referencedClass = constantPool.constantToString(constant)
        Console.out.println(s"Referenced class: $referencedClass")
      case _ =>
    }
  }

  override def visitMethod(method: Method): Unit = {
    val methodGen = new MethodGen(method, mainJavaClass.getClassName, constants)
    val methodVisitor = new MethodVisitor(methodGen, mainJavaClass)
    methodVisitor.start()
  }
}
