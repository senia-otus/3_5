package ru.otus.sc.config

import ru.otus.sc.db.DbConfig
import zio._
import zio.config._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe._

case class HttpConfig(host: String, port: Int)

case class RootConfig(
    db: DbConfig,
    http: HttpConfig
)

object RootConfig {

  val rootDescriptor: ConfigDescriptor[RootConfig] = descriptor[RootConfig]

  val live: Layer[ReadError[String], Has[RootConfig]] =
    TypesafeConfig.fromDefaultLoader(rootDescriptor)

  val noErrors: ULayer[Has[RootConfig]] = live.orDie

  type AllConfigs = Has[HttpConfig] with Has[DbConfig]

  val allConfigs: ULayer[AllConfigs] =
    noErrors >>> (
      subConfig(_.db) ++
        subConfig(_.http)
    )

  def subConfig[T: Tag](f: RootConfig => T): URLayer[Has[RootConfig], Has[T]] =
    ZLayer.fromService(f)
}
