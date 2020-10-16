import zio.ZIO
import zio.duration.durationInt

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

val myFuture = Future {
  Thread.sleep(100)
  1
}

val f2 = myFuture.map(1.+)
val f3 = f2.map(1.+)
//val f3_1 = f2.map(1.+)
val f4 = f3.map(1.+)
val f5 = f4.map(1.+)

val f6: Future[Int] = {
  val promise = Promise[Int]
  f5.onComplete {
    case Failure(exception) => promise.failure(exception)
    case Success(value)     => promise.success(value + 1)
  }
  promise.future
}

val z1 = ZIO.succeed(1).delay(1.second)
val z2 = z1.map(1.+)
val z3 = z2.map(1.+)
//val z3_1 = z2.map(1.+)
val z4 = z3.map(1.+)
val z5 = z4.map(1.+)
