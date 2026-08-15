package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.core.sounds.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

object FloorFlowingWater extends Module

def CatFloorFlowingWater(using Universe) =
  ComponentCategory("floorFlowingWater", "Floor flowing water")

@definition def flowingWaterInfos(using Universe) = FlowingWaterInfos()

@definition def platformGroundTemplate(using Universe) = PlatformGround().asTemplate()
@definition def platformWallTemplate(using Universe) = PlatformWall().asTemplate()
@definition def changeWaterFloorButtonTemplate(using Universe) = ChangeWaterFloorButton().asTemplate()

@definition def waterFlowingGround(using Universe) = WaterFlowingGround()
@definition def waterFlowingHole(using Universe) = WaterFlowingHole()

class FlowingWaterInfos(using ComponentInit) extends Component {
  category = CatFloorFlowingWater

  var waterFloor: Int = 2

  def setWaterFloor(value: Int): Unit = {
    if waterFloor == value then
      return

    for player <- universe.players do
      player.playSound(wtSounds.rushingWater)

    waterFloor = value

    for component <- Mazes.posComponentsBottomUp do {
      component match
        case component: PlatformGround =>
          component.position = component.position.map(_.withZ(value))
        case component: PlatformWall =>
          component.position = component.position.map(_.withZ(value - 1))
        case _ =>
          ()
    }
  }
}

class PlatformGround(using ComponentInit) extends PosComponent {
  category = CatFloorFlowingWater
  painter += "Fields/WoodFloor"

  override def hookEntering(context: EnteringContext): Unit = ()
  override def hookEntered(context: EnteredContext): Unit = ()
}

class PlatformWall(using ComponentInit) extends PosComponent {
  category = CatFloorFlowingWater
  painter += "Boxes/WoodenBox"

  override def hookEntering(context: EnteringContext): Unit =
    context.cancel()

  override def hookEntered(context: EnteredContext): Unit = ()
}

class ChangeWaterFloorButton(using ComponentInit) extends PushButton {
  category = CatFloorFlowingWater

  var newWaterFloor: Int = 1

  override def buttonDown(context: EnteredContext): Unit =
    super.buttonDown(context)
    flowingWaterInfos.setWaterFloor(newWaterFloor)
}

class WaterFlowingHole(using ComponentInit) extends Water {
  category = CatFloorFlowingWater
  painter += "Filters/NiceSoftDarken"

  private val blackBox = Shape.Box(
    Rectangle.sized(30, 30),
    Fill.Color(RGBA.Black),
    Stroke.None,
    Point(15, 15),
  )

  override protected def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    if context.where.forall(_.z == flowingWaterInfos.waterFloor) then
      super.doPresent(context)
    else
      blackBox +: DissipateNeighbors.presentDissipateGroundNeighbors(context)

  override def entering(context: EnteringContext): Unit =
    if flowingWaterInfos.waterFloor == context.pos.z then
      super.entering(context)

  override def entered(context: EnteredContext): Unit = {
    import context.*

    if flowingWaterInfos.waterFloor == pos.z then
      // Act as water
      super.entered(context)
    else if flowingWaterInfos.waterFloor < pos.z then
      // Drop down, like an EmptyField

      if pos.z <= 0 then
        return // just to be safe

      player.sleep(200)

      player.moveTo(pos.withZ(pos.z - 1), execute = true)
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.PassOver, player, pos, _, _) =>
      flowingWaterInfos.waterFloor <= pos.z && !player.isAbleTo(GoOnWater)
  }
}

class WaterFlowingGround(using ComponentInit) extends Ground {
  category = CatFloorFlowingWater
  painter += "Fields/Grass"

  var lightenColor = RGBA(1, 1, 1, 95.0 / 255.0)

  private def box = Shape.Box(
    Rectangle.sized(30, 30),
    Fill.Color(lightenColor),
    Stroke.None,
    Point(15, 15),
  )

  override protected def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    (super.doPresent(context) :+ box)
      ++ DissipateNeighbors.presentDissipateNeighbors(context, _ == grass)
}
