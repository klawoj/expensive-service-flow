package pl.klawoj.delay

import akka.stream.contrib.DelayFlow.DelayStrategy
import pl.klawoj.{FailedTask, Task, TaskDefinition}

import scala.concurrent.duration.FiniteDuration

class IncreaseDelayOnError(start: FiniteDuration, step: FiniteDuration, ErrorCode: Int) extends DelayStrategy[Task] {

  private var mul = 1
  private def duration: FiniteDuration = start + step * mul * mul / 2

  override def nextDelay(elem: Task): FiniteDuration = elem match {
    case _: TaskDefinition => duration
    case FailedTask(_, ErrorCode) =>
      mul = mul + 1
      duration
  }
}