package com.lectra.kapoeira.domain

import com.lectra.kapoeira.domain
import com.lectra.kapoeira.domain.Services.ReadHeaders

import java.nio.charset.StandardCharsets
import scala.util.Try

/** The record read from the cucumber Datatable directly, or a File.
  */
final case class RecordRead(
    topicAlias: String,
    key: String,
    value: Array[Byte],
    headers: Map[String, Any]
)

/** Interpolate variables of T with the backgroundContext.
  */
trait Interpolate[T] {
  def interpolate(t: T, backgroundContext: BackgroundContext): T
}

/** Map a T to a list of RecordRead. Used to map domain.Record ADT related to a file.
  */
trait RecordDataFromFile[T] {
  def readFromFile(t: T, f: String => List[String]): List[RecordRead]
}

/** Map a T to a list of RecordRead. Used to map domain.Record ADT related to a values in Datatable.
  */
trait RecordData[T] {
  def read(t: T): RecordRead
}

/** Facilitate import of all implicits.
  */
trait RecordReadImplicits
    extends InterpotaleImplicits
    with RecordDataImplicits
    with RecordDataFromFileImplicits

trait InterpotaleImplicits {

  implicit val interpolateString: Interpolate[String] =
    (t: String, ctx: BackgroundContext) => ctx.substituteVariablesIn(t)
  implicit val interpolateRecordRead: Interpolate[RecordRead] =
    (t: RecordRead, ctx: BackgroundContext) => {
      RecordRead(
        ctx
          .substituteVariablesIn(t.topicAlias),
        ctx
          .substituteVariablesIn(t.key),
        ctx
          .substituteVariablesIn(new String(t.value))
          .getBytes(StandardCharsets.UTF_8),
        interpolateMap(t.headers, ctx)
      )
    }

  //runtime inspection of iterable to interpolate ...
  private def interpolateIterable(
      xs: Iterable[Any],
      ctx: BackgroundContext
  ): Iterable[Any] =
    xs.map {
      case s: String           => ctx.substituteVariablesIn(s)
      case m: Map[String, Any] => interpolateMap(m, ctx)
      case x: Iterable[Any]    => interpolateIterable(x, ctx)
      case _                   => xs
    }

  //runtime inspection of Map to interpolate ...
  private def interpolateMap(
      map: Map[String, Any],
      ctx: BackgroundContext
  ): Map[String, Any] = map.map {
    case (k, v: String) =>
      (ctx.substituteVariablesIn(k), ctx.substituteVariablesIn(v))
    case (k, v: Map[String, Any]) =>
      (ctx.substituteVariablesIn(k), interpolateMap(v, ctx))
    case (k, v: List[Any]) =>
      (ctx.substituteVariablesIn(k), interpolateIterable(v, ctx))
    case (k, v) => (ctx.substituteVariablesIn(k), v)
  }

  implicit class InterpolateOps[T: Interpolate](val t: T) {
    def interpolate(backgroundContext: BackgroundContext): T =
      implicitly[Interpolate[T]].interpolate(t, backgroundContext)
  }
}

trait RecordDataFromFileImplicits {

  implicit def fileKeyValue(implicit
      readHeaders: ReadHeaders[String]
  ): RecordDataFromFile[FileKeyValueRecord] =
    new RecordDataFromFile[FileKeyValueRecord] {
      override def readFromFile(
          t: FileKeyValueRecord,
          f: String => List[String]
      ): List[RecordRead] = {
        f(t.file).map(line => {
          val columns = line.split(t.separator)
          domain.RecordRead(
            t.topicAlias,
            columns(0),
            columns(1).getBytes(StandardCharsets.UTF_8),
            Try(columns(2))
              .map(headersString => readHeaders.readHeaders(headersString))
              .getOrElse(Map.empty)
          )
        })
      }
    }

  implicit val fileValueRecord: RecordDataFromFile[FileValueRecord] =
    new RecordDataFromFile[FileValueRecord] {
      override def readFromFile(
          t: FileValueRecord,
          f: String => List[String]
      ): List[RecordRead] = f(t.file).map(line => {
        RecordRead(
          t.topicAlias,
          t.key,
          line.getBytes(StandardCharsets.UTF_8),
          Map.empty
        )
      })
    }

  implicit val fileFormattedValue
      : RecordDataFromFile[FileFormattedValueRecord] =
    new RecordDataFromFile[FileFormattedValueRecord] {
      override def readFromFile(
          t: FileFormattedValueRecord,
          f: String => List[String]
      ): List[RecordRead] = {
        val line = f(t.file).map(_.trim).mkString
        List(
          RecordRead(
            t.topicAlias,
            t.key,
            line.getBytes(StandardCharsets.UTF_8),
            Map.empty
          )
        )
      }
    }

  implicit class RecordDataFromFileOps[T: RecordDataFromFile](val t: T) {
    def readFromFile(f: String => List[String]): List[RecordRead] =
      implicitly[RecordDataFromFile[T]].readFromFile(t, f)
  }
}

trait RecordDataImplicits {

  implicit val keyValue: RecordData[KeyValueRecord] =
    new RecordData[KeyValueRecord] {
      override def read(t: KeyValueRecord): RecordRead = RecordRead(
        t.topicAlias,
        t.key,
        t.value.getBytes(StandardCharsets.UTF_8),
        t.headers
      )
    }

  implicit class RecordDataOps[T: RecordData](val t: T) {
    def read: RecordRead = implicitly[RecordData[T]].read(t)
  }

}
