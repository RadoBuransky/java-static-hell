package com.buransky.javaStaticHell

import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic._

import scala.collection.mutable.ListBuffer

private[javaStaticHell] class MethodVisitor(methodGen: MethodGen, javaClass: JavaClass) extends EmptyVisitor {
  private val buffer = new ListBuffer[String]
  def staticDependencies(): Iterable[String] = {
    if (!methodGen.isAbstract && !methodGen.isNative) {
      var instructionHandle = methodGen.getInstructionList.getStart
      while (instructionHandle != null) {
        val instruction = instructionHandle.getInstruction
        if (!visitInstruction(instruction))
          instruction.accept(this)

        instructionHandle = instructionHandle.getNext
      }
    }
    buffer.distinct
  }

  private def visitInstruction(instruction: Instruction): Boolean =
    InstructionConstants.INSTRUCTIONS(instruction.getOpcode) != null &&
    !instruction.isInstanceOf[ConstantPushInstruction] &&
    !instruction.isInstanceOf[ReturnInstruction]

  override def visitINVOKESTATIC(invoke: INVOKESTATIC): Unit = {
    val className = invoke.getReferenceType(methodGen.getConstantPool)
    val methodName = invoke.getMethodName(methodGen.getConstantPool)
    buffer += className.toString
  }
}
