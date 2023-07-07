package com.lectra.kapoeira.logging

import com.lectra.kapoeira.domain.Services.ZLogger
import com.typesafe.scalalogging.LazyLogging
import zio.{Has, Task, ULayer, ZIO, ZLayer}

object ZLoggerTypesafe {
  val live = new ZLogger with LazyLogging {

    override def info(message: String): Task[Unit] = ZIO.effect(logger.info(message))

    override def debug(message: String): Task[Unit] = ZIO.effect(logger.debug(message))

    override def warn(message: String): Task[Unit] = ZIO.effect(logger.warn(message))
  }
  val layer: ULayer[Has[ZLogger]] = ZLayer.succeed(live)

}
