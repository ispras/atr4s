package ru.ispras.atr.utils

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import java.security.MessageDigest

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.features.CachingFeature

import scala.reflect.ClassTag

/**
  * Stores and loads results of ATR step; each result is assumed to be uniquely determined by its configuration.
  *
  * @param config         configuration of the step to be cached/uncached
  * @param lastDirName    name of subdirectory (under cache/ directory) to store/load created objects
  * @param recreateObject function to create object (used in case of no stored cache)
  * @tparam C class of config
  * @tparam T class of object to be created (used in case of no stored cache)
  * @tparam P class of data storing parameters needed to create object
  */
class Cacher[C, T: ClassTag, P](config: C,
                                lastDirName: String,
                                recreateObject: P => T) {
  val log = LogManager.getLogger(getClass)
  val dirName = "cache/" + lastDirName

  def runtimeClassOf[A:ClassTag] = implicitly[ClassTag[A]].runtimeClass

  def encode(config: C, encoding: String = "MD5"): String = {
    val digest = MessageDigest.getInstance(encoding)
    val text2Encode = config.toString
    digest.digest(text2Encode.getBytes).map("%02x".format(_)).mkString
  }

  val cacheFileName: String =
    //prefix for disambiguating features with same params, but different names, and for easy search of cached feature
    (config match {
      case f: CachingFeature => f.innerFeature.id
      case c: Any => c.getClass.getSimpleName
    }) + encode(config) + ".cache"

  def getFromCache(originalParams: P): T = {
    val d = new File(dirName)
    val files: Seq[File] = if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.getName == cacheFileName).toSeq
    } else {
      d.mkdirs()
      Seq.empty
    }

    if (files.size < 1) {
      log.debug(s"No cache was found ($cacheFileName), creating it...")
      val obj = recreateObject(originalParams)
      log.debug(s"Object ${config.getClass.getSimpleName} was created, serializing it...")
      val oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(new File(dirName, cacheFileName))))
      oos.writeObject(obj)
      oos.close()
      JsonSer.writeFile(Seq(config), new java.io.File(dirName, cacheFileName + ".meta").getPath)
      log.debug(s"Object ($cacheFileName) was cached")
      obj
    } else {
      log.debug(s"Cache was found (fileName: $cacheFileName), loading it...")
      val ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(new File(dirName, cacheFileName))))
      val obj = ois.readObject().asInstanceOf[T]
      ois.close()
      log.debug("Object was deserialized")
      obj
    }
  }
}
