package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

object PlayerLifeManagement extends Module

@definition def lifePlugin(using Universe) = new LifePlugin
@definition def circleHolePlugin(using Universe) = new CircleHolePlugin
@definition def fallInWaterPlugin(using Universe) = new FallInWaterPlugin

class LifePlugin(using ComponentInit) extends PlayerPlugin {
  icon += "BodyParts/Heart"

  @noinspect
  var revivePos: Option[SquareRef] = None

  override protected def startGame(): Unit =
    revivePos = universe.players.head.reified[Player].position

  override def entered(context: EnteredContext): Unit = {
    import context.*
    optSrc match
      case Some(src) if src.zone == pos.zone && src.map == pos.map =>
        ()
      case _ =>
        revivePos = Some(pos)
  }

  def lightRevive(player: Player): Unit = {
    player.position = revivePos
    for _ <- 0 to 5 do
      player.hide()
      player.sleep(100)
      player.show()
      player.sleep(100)
  }
}

class CircleHolePlugin(using ComponentInit) extends PlayerPlugin {
  icon += "Holes/CircleHole"
  painterAbove += "Holes/CircleHole"
}

class FallInWaterPlugin(using ComponentInit) extends PlayerPlugin {
  icon += "Fields/Water"
  icon += "Holes/CircleHole"

  var didGoOnWater: Boolean = false

  override def perform(player: CorePlayer): CorePlayer.Perform = {
    case GoOnWater =>
      didGoOnWater = true
  }

  override def exiting(context: ExitingContext): Unit = {
    import context.*
    if player has buoys then
      player.plugins -= this
  }

  override def entered(context: EnteredContext): Unit = {
    import context.*
    if didGoOnWater then
      didGoOnWater = false
      player.plugins += circleHolePlugin
      temporize()
      player.plugins -= circleHolePlugin
      lifePlugin.lightRevive(player)
  }
}
