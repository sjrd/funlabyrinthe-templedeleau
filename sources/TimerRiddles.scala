package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

import user.sjrd.mapzones.*
import user.sjrd.stopwatches.*

object TimerRiddles extends Module

@definition def timerRiddleTemplate(using Universe) = TimerRiddle().asTemplate()

class TimerRiddle(using ComponentInit) extends ZoneEvents {
  private val stopwatch = subComponent(new TimerRiddleStopwatch(this))
  private val plugin = subComponent(new TimerRiddlePlugin(this))

  category = ComponentCategory("timerRiddles", "Timer riddles")
  icon += "Miscellaneous/QuestionMark"

  var target: Position = Position.Zero
  var availableTime: Int = 1000

  @noinspect
  var solved: Boolean = false
  @noinspect
  var savedTargetSquare: Option[Square] = None

  override protected def startGame(): Unit = {
    super.startGame()
    for map <- zoneMap do
      savedTargetSquare = Some(map(target))
  }

  override def enteredZone(context: EnteredContext): Unit = {
    import context.*

    if !solved && savedTargetSquare.isDefined then
      val targetRef = map.ref(target)
      targetRef() = savedTargetSquare.get

      player.plugins += plugin
      stopwatch.start(player.corePlayer, availableTime)
  }

  override def exitedZone(context: ExitedContext): Unit =
    stopwatch.stop(context.player.corePlayer)
    context.player.plugins -= plugin

  def entering(context: EnteringContext): Unit = {
    import context.*
    if pos.pos == target then
      stopwatch.stop(player.corePlayer)
      solved = true
      player.plugins -= plugin
  }

  def stopwatchExpired(player: CorePlayer): Unit = {
    // failed the riddle
    val map = zoneMap.get
    map(target) = map(target).field
    player.plugins -= plugin
  }
}

class TimerRiddleStopwatch(using ComponentInit)(riddle: TimerRiddle) extends Stopwatch {
  override def expired(player: CorePlayer): Unit =
    riddle.stopwatchExpired(player)
}

class TimerRiddlePlugin(using ComponentInit)(riddle: TimerRiddle) extends PlayerPlugin {
  override def entering(context: EnteringContext): Unit =
    riddle.entering(context)
}
