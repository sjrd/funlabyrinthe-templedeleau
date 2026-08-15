package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

import user.sjrd.movableblocks.*

object WTMovableBlocks extends Module

@definition def wtMovableBlockTemplate(using Universe) = WTMovableBlock().asTemplate()

@definition def wtMovableBlockPuzzle1(using Universe) = WTMovableBlockPuzzle1()
@definition def wtMovableBlockKeyPuzzle(using Universe) = WTMovableBlockKeyPuzzle()

@definition def wtResetBlocksPlugin(using Universe) = WTResetBlocksPlugin()

class WTMovableBlock(using ComponentInit) extends ConstrainedMovableBlock {
  painter = painter.empty + "Blocks/SquareBlock"
  maximumMoveCount = 1

  override protected def isDestSquareValid(square: Square): Boolean = square match
    case Square(_: Ground, e, t, o) => e.isEmpty && t.isEmpty && o.isEmpty
    case _                          => false

  override protected def isMoveAllowed(context: EnteringContext, target: SquareRef): Boolean = {
    import context.*

    super.isMoveAllowed(context, target) && {
      if flowingWaterInfos.waterFloor > pos.z then
        player.showMessageOnce(
          "Sous l'eau, tu n'as pas de point d'appui. "
            + "Tu ne peux donc pas pousser les blocs."
        )
        false
      else
        true
    }
  }

  override protected def applyMove(context: EnteringContext, target: SquareRef): Unit = {
    super.applyMove(context, target)
    target().field match
      case _: WaterFlowingHole => position = None
      case _                   => ()
  }
}

class WTMovableBlockPuzzle1(using ComponentInit) extends WTMovableBlock {
  allowedDirs = Set(Direction.East)

  override protected def applyMove(context: EnteringContext, target: SquareRef): Unit = {
    super.applyMove(context, target)
    fixThere()
    context.map(19, 18, 3) = grass
    context.player.playSound(wtSounds.success)
  }
}

class WTMovableBlockKeyPuzzle(using ComponentInit) extends WTMovableBlock {
  allowedDirs = Set(Direction.North)

  override protected def applyMove(context: EnteringContext, target: SquareRef): Unit = {
    super.applyMove(context, target)
    fixThere()
    context.map(48, 13, 2) += silverKey
    context.player.playSound(wtSounds.success)
  }
}

class WTResetBlocksPlugin(using ComponentInit) extends PlayerPlugin {
  override def entered(context: EnteredContext): Unit = {
    import context.*
    if !optSrc.exists(_.zone == pos.zone) then
      // We changed zones; reset all blocks
      for block <- universe.components[WTMovableBlock] do
        block.reset()
  }
}
