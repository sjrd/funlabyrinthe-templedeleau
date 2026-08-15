package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

import user.sjrd.chests.*

object WTMiscellaneous extends Module

@definition def closedTreasure(using Universe) = ClosedTreasure()

class ClosedTreasure(using ComponentInit) extends ClosedChest {
  override def pushing(context: EnteringContext): Unit = {
    import context.*

    cancel()
    pos() += noObstacle
    pos() += treasure
    player.win()
    player.playSound(wtSounds.openChest)
    player.showMessage("Félicitations ! Tu as trouvé le trésor !")
  }
}
