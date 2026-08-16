package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

import user.sjrd.movableblocks.*

object WTButtonsAndRocks extends Module

def CatButtonsAndRocks(using Universe) = ComponentCategory("buttonsAndRocks", "Buttons and Rocks")

@definition def bigRockTemplate(using Universe) = BigRock().asTemplate()
@definition def resetBigRockTemplate(using Universe) = ResetBigRock().asTemplate()
@definition def openerButtonTemplate(using Universe) = OpenerButton().asTemplate()

class BigRock(using ComponentInit) extends ConstrainedMovableBlock {
  category = CatButtonsAndRocks
  painter = painter.empty + "Rocks/BigRock"
  canCrossZones = true

  override protected def isMoveAllowed(context: EnteringContext, target: SquareRef): Boolean = {
    import context.*

    val targetSq = target()
    val fieldOK = targetSq.field match {
      case _:Ground | _:WaterFlowingHole => true
      case field: OneWayDoor             => field.allowedDir == player.direction
      case _                             => false
    }
    if !fieldOK || targetSq.obstacle != noObstacle then
      return false
    if target.posComponentsBottomUp.nonEmpty then
      return false

    ensureNotUnderwaterToPushBlock(player, pos)
  }

  override protected def applyMove(context: EnteringContext, target: SquareRef): Unit = {
    import context.*

    val dir = player.direction
    var actualTarget = target

    // Follow one-way door
    actualTarget().field match {
      case _: OneWayDoor => actualTarget = actualTarget +> dir
      case _             => ()
    }

    // Fall through holes
    while (actualTarget().field match {
      case _: WaterFlowingHole => actualTarget = actualTarget - (0, 0, 1); true
      case _                   => false
    }) do ()

    // Actual move
    val oldPos = position.get
    super.applyMove(context, actualTarget)

    // Sound
    if actualTarget.z < oldPos.z then
      if actualTarget.z < flowingWaterInfos.waterFloor then
        player.playSound(wtSounds.fallInWater)
      else
        player.playSound(wtSounds.boulderLanding)

    // Handle interactions with buttons
    oldPos().effect match {
      case button: PushButton if button.enabled =>
        val exitedContext = ExitedContext(player, oldPos)
        button.buttonUp(exitedContext)
      case _ =>
        ()
    }
    actualTarget().effect match {
      case button: PushButton if button.enabled =>
        val enteredContext = EnteredContext(player, actualTarget, Some(oldPos))
        button.buttonDown(enteredContext)
      case _ =>
        ()
    }
  }
}

class ResetBigRock(using ComponentInit) extends PushButton {
  category = CatButtonsAndRocks
  var rock: Option[BigRock] = None

  override def buttonDown(context: EnteredContext): Unit = {
    for oldPos <- rock.flatMap(_.position) do
      oldPos().effect match {
        case button: PushButton if button.enabled =>
          val exitedContext = ExitedContext(context.player, oldPos)
          button.buttonUp(exitedContext)
        case _ =>
          ()
      }

    rock.foreach(_.reset())
  }
}

class OpenerButton(using ComponentInit) extends PushButton {
  category = CatButtonsAndRocks

  var target: Position = Position.Zero
  var requiredButtonCount: Int = 1
  var permanent: Boolean = false

  @noinspect
  var downButtonCount: Int = 0
  @noinspect
  var savedSquare: Option[Square] = None

  override def buttonDown(context: EnteredContext): Unit = {
    import context.*
    downButtonCount += 1
    if downButtonCount == requiredButtonCount then
      player.playSound(wtSounds.success)
      savedSquare = Some(map(target))
      map(target) = grass
      if permanent then
        enabled = false
  }

  override def buttonUp(context: ExitedContext): Unit = {
    import context.*
    downButtonCount -= 1
    if downButtonCount == requiredButtonCount - 1 then
      map(target) = savedSquare.get
  }
}
