package pl.klawoj.delay

import java.time.{Duration, Instant}

import akka.stream.contrib.DelayFlow.DelayStrategy

import scala.concurrent.duration
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class DelayIfToManyInDuration[T](elements: Int, duration: Duration) extends DelayStrategy[T] {
  assert(elements >= 1)

  import DelayIfToManyInDuration._

  private var window = TimeWindow(duration)

  override def nextDelay(elem: T): FiniteDuration = {
    if (window.hasEnded) {
      window = TimeWindow(duration).withElementPassed
      passImmediately
    } else if (window.countReached(elements)) {
      val delayUntilNextWindow = window.timeUntilNextWindow
      window = window.next(duration).withElementPassed
      delayUntilNextWindow
    } else {
      window = window.withElementPassed
      passImmediately
    }
  }
}
object DelayIfToManyInDuration {

  val passImmediately = 0.seconds

  case class TimeWindow(end: Instant, passedElements: Int = 0) {

    def hasEnded: Boolean = end.isBefore(Instant.now())

    def countReached(elements: Int): Boolean = passedElements >= elements

    def withElementPassed = TimeWindow(end, passedElements + 1)

    def next(duration: Duration): TimeWindow = TimeWindow(end.plus(duration))

    def timeUntilNextWindow: FiniteDuration = duration.Duration.fromNanos(Duration.between(Instant.now(), end).toNanos)


  }
  object TimeWindow {
    def apply(duration: Duration): TimeWindow = TimeWindow(Instant.now().plus(duration))
  }

}
