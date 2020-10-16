package ru.otus.sc.utils
import zio.ZIO
import zio.clock.Clock
import zio.logging._
import zio.duration._

object LoggingUtils {
  def logger[R <: Logging, E, A1](name: String, names: String*)(
      zio: ZIO[R, E, A1]
  ): ZIO[Logging with R, E, A1] =
    log.locally(LogAnnotation.Name(name :: names.toList))(zio)

  class LocalTimedHelper(name: String) {
    def apply[R <: Logging with Clock, E, A1](names: String*)(
        zio: ZIO[R, E, A1]
    ): ZIO[Logging with Clock with R, E, A1] = localTimed(name, names: _*)(zio)
  }

  def localTimedNamed(name: String) = new LocalTimedHelper(name)

  def localTimed[R <: Logging with Clock, E, A1](name: String, names: String*)(
      zio: ZIO[R, E, A1]
  ): ZIO[Logging with Clock with R, E, A1] =
    logger(name, names: _*) {
      for {
        _     <- log.debug("Started")
        tuple <- zio.timed
        (duration, res) = tuple
        _ <- log.debug(s"Ended. Duration: ${duration.render}")
      } yield res
    }
}
