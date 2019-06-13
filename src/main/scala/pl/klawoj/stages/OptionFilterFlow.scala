package pl.klawoj.stages

import akka.NotUsed
import akka.stream.scaladsl.Flow

object OptionFilterFlow {
  def flow[T]: Flow[Option[T], T, NotUsed] =
    Flow[Option[T]] collect {
      case Some(v) ⇒ v
    }
}
