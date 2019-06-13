package pl.klawoj

import java.time.Duration

import akka.NotUsed
import akka.stream.ClosedShape
import akka.stream.contrib.DelayFlow
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, MergePreferred, RunnableGraph, Sink, Source, UnzipWith}
import pl.klawoj.ExpensiveServiceFlow.RequestsContract
import pl.klawoj.HttpErrorCodes.TOO_MANY_REQUESTS
import pl.klawoj.delay.{DelayIfToManyInDuration, IncreaseDelayOnError}
import pl.klawoj.stages.{ForceFrequency, OptionFilterFlow}

import scala.concurrent.duration.DurationLong

class ExpensiveServiceFlow[M1, M2, M3](
    source: Source[Task, M1],
    ackSink: Sink[Ack, M2],
    resultSink: Sink[TaskResult, M3],
    expensiveService: Task => ExpensiveServiceResponse,
    contract: RequestsContract = RequestsContract.default) {

  val callServiceFlow: Flow[Task, (TaskDefinition, ExpensiveServiceResponse), NotUsed] = Flow.fromFunction(t => (t.definition, expensiveService(t)))

  val forceFrequencyFlow = ForceFrequency(Task.frequencies, Task.frequencyMapping)


  def graph: RunnableGraph[(M2, M3)] = RunnableGraph.fromGraph {
    GraphDSL.create(source, ackSink, resultSink)((_, ack, completedTasks) => (ack, completedTasks)) {
      implicit b: GraphDSL.Builder[(M2, M3)] =>
        (in, acks, completedTasks) =>
          import GraphDSL.Implicits._

          val forceFrequency = b.add(forceFrequencyFlow)

          val service = b.add(callServiceFlow)

          val ok = b.add(OptionFilterFlow.flow[Ok].map(_.result))
          val failed = b.add(OptionFilterFlow.flow[FailedTask])

          val retryFailed = b.add(MergePreferred[Task](1))

          val delay = b.add(new DelayFlow[Task](() => new IncreaseDelayOnError(start = 0 seconds, step = 100 millis, ErrorCode = TOO_MANY_REQUESTS)))

          val throttleMonthly = b.add(new DelayFlow[Task](() => new DelayIfToManyInDuration[Task](contract.elements, contract.inDuration)))

          val responsePartition = b.add(UnzipWith[(TaskDefinition, ExpensiveServiceResponse), Option[Ok], Option[FailedTask]] {
            case (_: TaskDefinition, ok: Ok) => (Some(ok), None)
            case (t: TaskDefinition, error: Error) => (None, Some(FailedTask(t, error.errorCode)))
          })

          val broadcastResult = b.add(Broadcast[TaskResult](2))

          val toAck = b.add(Flow[TaskResult].map(r => Ack(r.task.id)))

          //flow composition
          in ~> forceFrequency ~> throttleMonthly ~> retryFailed ~> delay ~> service

          responsePartition.in <~ service

          broadcastResult <~ ok <~ responsePartition.out0

          acks <~ toAck <~ broadcastResult

          completedTasks <~ broadcastResult

          retryFailed.preferred <~ failed <~ responsePartition.out1

          ClosedShape
    }
  }
}
object ExpensiveServiceFlow {

  case class RequestsContract(elements: Int, inDuration: Duration)
  object RequestsContract {
    def default = RequestsContract(100000, Duration.ofDays(30))
  }

}



