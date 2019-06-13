package pl.klawoj

sealed trait Task {
  def definition: TaskDefinition

  def id: String = definition.id

  def priority: Int = definition.priority
}

case class TaskDefinition(override val id: String, override val priority: Int) extends Task {
  assert(priority >= 1)
  override def definition: TaskDefinition = this
}

case class FailedTask(definition: TaskDefinition, errorCode: Int) extends Task

object Task {
  def apply(id: String, priority: Int): Task = TaskDefinition(id, priority)

  def frequencyMapping: Task => Int = t => t.priority match {
    case p if p >= frequencies.size => frequencies.size - 1
    case p => p - 1
  }

  val frequencies: Seq[Int] = Seq(5, 3, 2)
}


sealed trait ExpensiveServiceResponse
case class Error(errorCode: Int) extends ExpensiveServiceResponse
case class Ok(result: TaskResult) extends ExpensiveServiceResponse

case class TaskResult(task: Task, result: String)

case class Ack(id: String)