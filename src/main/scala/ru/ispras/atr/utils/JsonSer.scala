package ru.ispras.atr.utils

import java.io.{File, FileReader, FileWriter}

import org.json4s.JsonAST.{JDouble, JString}
import org.json4s.jackson.Serialization._
import org.json4s.reflect.TypeInfo
import org.json4s.{MappingException, _}
import ru.ispras.atr.ATRConfig
import ru.ispras.atr.datamodel.DataConfig

/**
  * TODO
  * @author yaroslav
  */
object JsonSer {
  implicit val formats = DefaultFormats +
    ShortTypeHints(ATRConfig.subclasses ++ DataConfig.subclasses) + DoubleNanSerializer

  def readString[A](str: String)(implicit mf: Manifest[A]): A = {
    read[A](str)
  }

  def readFile[A](file: String)(implicit mf: Manifest[A]): A = {
    read[A](new FileReader(file))
  }

  def writeString[A <: AnyRef](a: A): String = {
    writePretty[A](a)
  }

  def writeCompactString[A <: AnyRef](a: A): String = {
    write[A](a)
  }

  def writeFile[A <: AnyRef](a: A, file: String) = {
    val fw = new FileWriter(file)
    writePretty(a, fw)
    fw.close()
  }

  def writeJsonLines[A <: AnyRef](iter: Iterable[A], file: File) = {
    val fw = new FileWriter(file)
    try {
      iter.foreach { elem =>
        val str = JsonSer.writeCompactString(elem)
        fw.write(s"$str\n")
      }
    } finally {
      fw.close()
    }
  }

  def toJValue[A <: AnyRef](a: A) = {
    import org.json4s.jackson.JsonMethods._
    parse(writeCompactString(a))
  }
}

object DoubleNanSerializer extends Serializer[NanDouble] {
  private val NanDoubleClass = classOf[NanDouble]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), NanDouble] = {
    case (TypeInfo(NanDoubleClass, _), json) => json match {
      case JDouble(d) => NanDouble(d)
      case JString("NaN") => NanDouble(Double.NaN)
      case x => throw new MappingException("Can't convert " + x + " to NanDouble")
    }
  }

  def serialize(implicit formats: Formats): PartialFunction[Any, JValue] = {
    case NanDouble(Double.NaN) => JString("NaN")
    case NanDouble(d) => JDouble(d)
  }
}

case class NanDouble(value: Double)
object NanDouble {
  implicit def fromDouble(value: Double) = new NanDouble(value)
}