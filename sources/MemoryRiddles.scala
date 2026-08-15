package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

import user.sjrd.mapzones.*

object MemoryRiddles extends Module

@definition def memoryRiddlePath(using Universe) = MemoryRiddlePath()
@definition def hiddenMemoryRiddlePath(using Universe) = HiddenMemoryRiddlePath()

@definition def memoryRiddleTemplate(using Universe) = MemoryRiddle().asTemplate()

class MemoryRiddlePath(using ComponentInit) extends Effect {
  category = ComponentCategory("memoryRiddles", "Memory riddles")
  painter += "Filters/YellowLighten"
}

class HiddenMemoryRiddlePath(using ComponentInit) extends Effect {
  category = ComponentCategory("memoryRiddles", "Memory riddles")
}

class MemoryRiddle(using ComponentInit) extends ZoneEvents {
  private val plugin = subComponent(new MemoryRiddlePlugin(this))

  category = ComponentCategory("memoryRiddles", "Memory riddles")
  icon += "Miscellaneous/QuestionMark"

  var target: Position = Position.Zero

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

      for
        range <- zoneRange
        ref <- range
      do
        if ref().effect == hiddenMemoryRiddlePath then
          ref() += memoryRiddlePath

      player.sleep(1500)

      for
        range <- zoneRange
        ref <- range
      do
        if ref().effect == memoryRiddlePath && ref != pos then
          ref() += hiddenMemoryRiddlePath

      player.plugins += plugin
  }

  override def exitedZone(context: ExitedContext): Unit =
    context.player.plugins -= plugin
}

class MemoryRiddlePlugin(using ComponentInit)(riddle: MemoryRiddle) extends PlayerPlugin {
  override def entering(context: EnteringContext): Unit = {
    import context.*
    if pos.pos == riddle.target then
      riddle.solved = true
      player.plugins -= this
  }

  override def exited(context: ExitedContext): Unit = {
    import context.*

    optDest match
      case Some(dest) if dest().effect == hiddenMemoryRiddlePath =>
        // one step
        dest() += memoryRiddlePath
      case _ =>
        // failed the riddle
        println(s"failed, replacing ${map(riddle.target)} at ${riddle.target}")
        map(riddle.target) = map(riddle.target).field
        player.plugins -= this
  }
}
