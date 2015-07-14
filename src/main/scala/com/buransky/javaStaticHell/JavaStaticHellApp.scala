package com.buransky.javaStaticHell

import java.io.File
import java.util.jar.JarFile
import org.apache.bcel.classfile.ClassParser

import scala.collection.JavaConversions._

/**
 * Application command line arguments.
 *
 * @param jarFilePath Path to jar file to be analysed.
 */
case class JavaStaticHellAppArgs(jarFilePath: File) {
  if (!jarFilePath.exists())
    throw new IllegalArgumentException(s"JAR file does not exist! [$jarFilePath]")
}

/**
 * Application entry point.
 */
object JavaStaticHellApp {
  def main(args: Array[String]) = {
    // Get application arguments
    val appArgs = getAppAgrs(args)

    // Go through all entries
    val jarEntries = new JarFile(appArgs.jarFilePath).entries()
    while (jarEntries.hasMoreElements) {
      // Get next entry
      val jarEntry = jarEntries.nextElement()
      if (!jarEntry.isDirectory && jarEntry.getName.endsWith(".class")) {
        val classParser = new ClassParser(appArgs.jarFilePath.getAbsolutePath, jarEntry.getName)
        val classVisitor = new ClassVisitor(classParser.parse())
        classVisitor.start()
      }
    }
  }

  private def getAppAgrs(args: Array[String]) =
    JavaStaticHellAppArgs(new File(args(0)))
}
