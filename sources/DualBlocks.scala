package user.sjrd.templedeleau

import scala.util.chaining.*

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

object DualBlocks extends Module {
  override def dependsOn: Set[Module] = Set(Mazes)
}

@definition def dualSilverBlock(using Universe) = DualBlock().tap { b =>
  b.painter = silverBlock.painter
  b.lock = silverBlock.lock
  b.message = silverBlock.message
}

@definition def dualGoldenBlock(using Universe) = DualBlock().tap { b =>
  b.painter = goldenBlock.painter
  b.lock = goldenBlock.lock
  b.message = goldenBlock.message
}

class DualBlock(using ComponentInit) extends Block {
  override def pushing(context: EnteringContext): Unit = {
    import context.*

    super.pushing(context)

    if pos().obstacle == noObstacle then
      for dir <- Direction.values do
        if (pos +> dir)().obstacle == this then
          (pos +> dir)() += noObstacle
  }
}
