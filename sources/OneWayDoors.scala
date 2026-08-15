package user.sjrd.templedeleau

import scala.util.chaining.*

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

object OneWayDoors extends Module

inline val OneWayDoorTemporization = 250

@definition def northOneWayDoor(using Universe) = OneWayDoor().tap { d =>
  d.allowedDir = Direction.North
  d.painter += "Gates/StealGateNorth"
}

@definition def eastOneWayDoor(using Universe) = OneWayDoor().tap { d =>
  d.allowedDir = Direction.East
  d.painter += "Gates/StealGateEast"
}

@definition def southOneWayDoor(using Universe) = OneWayDoor().tap { d =>
  d.allowedDir = Direction.South
  d.painter += "Gates/StealGateSouth"
}

@definition def westOneWayDoor(using Universe) = OneWayDoor().tap { d =>
  d.allowedDir = Direction.West
  d.painter += "Gates/StealGateWest"
}

class OneWayDoor(using ComponentInit) extends Field {
  var allowedDir: Direction = Direction.North
  var openField: Option[Field] = None

  override def entering(context: EnteringContext): Unit = {
    import context.*

    if player.direction != allowedDir then
      cancel()
  }

  override def entered(context: EnteredContext): Unit = {
    context.goOnMoving = true
    context.temporization = OneWayDoorTemporization
  }

  override def exited(context: ExitedContext): Unit =
    context.player.playSound(wtSounds.closeOneWayDoor)

  override def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    openField match
      case Some(altField) if context.where.exists(_.posComponentsBottomUp.nonEmpty) =>
        altField.present(context)
      case _ =>
        super.doPresent(context)
}
