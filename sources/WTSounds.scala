package user.sjrd.templedeleau

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.sounds.*

object WTSounds extends Module

@definition def wtSounds(using Universe) = WTSounds()

class WTSounds(using ComponentInit) extends Component {
  icon += "Music/Speaker"

  var fallInWater = Sound("Plouf")
  var success = Sound("Reward")
  var rushingWater = Sound("RushingWater")
  var closeOneWayDoor = Sound("CloseOneWayDoor")
  var openChest = Sound("OpenChest")
  var dive = Sound("Dive")
}
