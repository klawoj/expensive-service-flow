package pl.klawoj.stages

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.collection.immutable

object AccumulateUntil {

  def apply[Element](condition: Seq[Element] => Boolean) = new AccumulateUntil[Element](condition)

}

final class AccumulateUntil[Element](condition: Seq[Element] => Boolean) extends GraphStage[FlowShape[Element, immutable.Seq[Element]]] {

  val in = Inlet[Element]("AccumulateUntil.in")
  val out = Outlet[immutable.Seq[Element]]("AccumulateUntil.out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    private var buffer = List.empty[Element]

    setHandlers(in, out, new InHandler with OutHandler {

      override def onPush(): Unit = {
        val nextElement = grab(in)
        buffer = nextElement :: buffer
        if (condition(buffer)) {
          push(out, buffer.reverse)
          buffer = List.empty
        } else {
          pull(in)
        }
      }

      override def onPull(): Unit = {
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        buffer = List.empty[Element]
        completeStage()
      }
    })

    override def postStop(): Unit = {
      buffer = List.empty[Element]
    }
  }
}
