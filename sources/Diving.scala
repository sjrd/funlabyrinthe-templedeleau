package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

import user.sjrd.stopwatches.*
import user.sjrd.underwater.*

object Diving extends Module

case object BreatheUnderwater extends Ability derives Pickleable, Inspectable

def CatDiving(using Universe) = ComponentCategory("diving", "Diving")

@definition def divePosition(using Universe) =
  Attribute.create[Position](Position.Zero)

@definition def diveManagementPlugin(using Universe) = DiveManagementPlugin()
@definition def breathingStopwatch(using Universe) = BreathingStopwatch()
@definition def airBubbles(using Universe) = AirBubbles()
@definition def flippers(using Universe) = Flippers()
@definition def scubaTanks(using Universe) = ScubaTanks()

@definition def flippersTool(using Universe) = ItemTool.make(
  flippers,
  "Tu as trouvé des palmes. "
    + "Tu vas pouvoir plonger sous l'eau. "
    + "Appuye sur X quand tu es sur l'eau profonde pour plonger. "
    + "Sur une zone plus claire dans l'eau, utilise C pour remonter à la surface.",
)

@definition def scubaTank(using Universe) = ItemTool.make(
  scubaTanks,
  "Tu as trouvé une bouteille d'oxygène. "
    + "Tu pourras maintenant respirer sous l'eau.",
)

class DiveManagementPlugin(using ComponentInit) extends PlayerPlugin {
  category = CatDiving

  var breathingTime: Int = 15000

  override def perform(player: CorePlayer) = {
    case GoOnWater => ()
  }

  override def entered(context: EnteredContext): Unit = {
    import context.*

    if optSrc.isEmpty then
      return
    val src = optSrc.get

    val wasInWater = src.z < flowingWaterInfos.waterFloor
    val isInWater = pos.z < flowingWaterInfos.waterFloor

    (wasInWater, isInWater) match {
      case (false, true) =>
        // The player enters into the water
        player.attributes(divePosition) = src.pos
        player.playSound(wtSounds.dive)
        player.plugins += underwaterPlugin

        if player cannot BreatheUnderwater then
          player.showMessageOnce(
            "Tu ne peux pas respirer sous l'eau, donc tu as un temps limité ! "
              + "Pense à remonter à temps à la surface avec C."
          )
          breathingStopwatch.start(player.corePlayer, breathingTime)

      case (true, false) =>
        // The player comes back to the surface
        breathingStopwatch.stop(player.corePlayer)
        player.plugins -= underwaterPlugin

      case _ =>
        () // nothing changes
    }
  }

  override def onKeyEvent(corePlayer: CorePlayer, event: KeyEvent): Unit = {
    val player = corePlayer.reified[Player]
    if player.position.isEmpty || event.hasAnyControlKey then
      ()
    else
      event.keyString.toUpperCase() match {
        case "X" => moveUpDown(player, isUp = false, event)
        case "C" => moveUpDown(player, isUp = true, event)
        case _   => ()
      }
  }

  private def moveUpDown(player: Player, isUp: Boolean, keyEvent: KeyEvent): Unit = {
    // Compute dest and up
    val pos = player.position.get
    val dest = pos +> (if isUp then Direction3D.Up else Direction3D.Down)
    val up = if dest.z > pos.z then dest else pos

    // Check that there is hole through which water flows upwards
    up().field match {
      case _: WaterFlowingHole if up.z <= flowingWaterInfos.waterFloor =>
        // Go up as if by Player.move()
        if player.testMoveAllowed(dest, player.direction, Some(keyEvent)) then
          player.moveTo(dest, execute = false) // TODO actually execute?

      case _ =>
        ()
    }
  }
}

class BreathingStopwatch(using ComponentInit) extends Stopwatch {
  category = CatDiving

  override def expired(corePlayer: CorePlayer): Unit = {
    corePlayer.enqueueUnderControl { () =>
      if corePlayer cannot BreatheUnderwater then {
        // Revive the player where they dove
        val player = corePlayer.reified[Player]
        player.plugins -= underwaterPlugin
        lifePlugin.revivePos = player.position.map(_.withPos(player.attributes(divePosition)))
        lifePlugin.lightRevive(player)
      }
    }
  }
}

class AirBubbles(using ComponentInit) extends Effect {
  category = CatDiving
  painter += "Miscellaneous/AirBubbles"

  // don't display the bubbles if they are above awater
  override def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    import context.*
    if universe.isEditing || pos.forall(_.z < flowingWaterInfos.waterFloor) then
      super.doPresent(context)
    else
      Batch.empty

  override def entered(context: EnteredContext): Unit =
    breathingStopwatch.stop(context.player.corePlayer)

  override def exited(context: ExitedContext): Unit =
    import context.*
    if (player cannot BreatheUnderwater) && pos.z < flowingWaterInfos.waterFloor then
      breathingStopwatch.start(player.corePlayer, diveManagementPlugin.breathingTime)
}

class Flippers(using ComponentInit) extends ItemDef {
  category = CatDiving
  icon += "Objects/Flippers"

  override protected def countChanged(player: CorePlayer, previousCount: Int, newCount: Int): Unit =
    if newCount > 0 then
      player.plugins += diveManagementPlugin
    else
      player.plugins -= diveManagementPlugin
}

class ScubaTanks(using ComponentInit) extends ItemDef {
  category = CatDiving
  icon += "Objects/ScubaTank"

  override protected def countChanged(player: CorePlayer, previousCount: Int, newCount: Int): Unit =
    if newCount > 0 then
      breathingStopwatch.stop(player)

  override def perform(player: CorePlayer) = {
    case BreatheUnderwater if player has this => ()
  }
}
