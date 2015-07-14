package com.buransky.javaStaticHell

import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic._

private[javaStaticHell] class MethodVisitor(methodGen: MethodGen, javaClass: JavaClass) extends EmptyVisitor {
  def start(): Unit = {
    if (!methodGen.isAbstract && !methodGen.isNative) {
      var instructionHandle = methodGen.getInstructionList.getStart
      while (instructionHandle != null) {
        val instruction = instructionHandle.getInstruction
        if (!visitInstruction(instruction))
          instruction.accept(this)

        instructionHandle = instructionHandle.getNext
      }
    }
  }

  private def visitInstruction(instruction: Instruction): Boolean =
    InstructionConstants.INSTRUCTIONS(instruction.getOpcode) != null &&
    !instruction.isInstanceOf[ConstantPushInstruction] &&
    !instruction.isInstanceOf[ReturnInstruction]

  override def visitINVOKEVIRTUAL(invoke: INVOKEVIRTUAL): Unit = {
    val className = invoke.getReferenceType(methodGen.getConstantPool)
    val methodName = invoke.getMethodName(methodGen.getConstantPool)
    Console.out.println(s"Invoke virtual: $className.$methodName")
  }
}
