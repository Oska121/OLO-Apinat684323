package GUI

import gameEngine._
import scala.swing._
import java.awt.{Color, Graphics2D, Point, Graphics}
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import event._
import gameEngine.Pos
import gameEngine.World
import scala.math

import scala.collection.mutable.Buffer



class PaintWorld() extends Panel {
  
  //creating game:
  val world = new World(TankGame.WorldWidth, TankGame.WorldHeight)
  
  world.createTank("Player")
  world.createTank("AI")
  
  world.addAmmosToTanks(10)
  
  val explosions = Buffer.empty[ExplosionAnimation]
  
  override def paintComponent(g: Graphics2D) {
    
    val gamefield = world.gamefield
    
    //clearing screen before redraw
    g.clearRect(0, 0, Help.WorldXToUI(TankGame.WorldWidth), Help.ScaleWorldYToUI(TankGame.WorldHeight))
    g.setBackground(Color.WHITE)
    
    
    //drawing info panel
    
    //drawing frame:
    g.setColor(Color.BLACK)
    g.drawRect(0, 0, Help.WorldXToUI(TankGame.WorldWidth), TankGame.InfoPanelHeight)
    val fract = Help.WorldXToUI(TankGame.WorldWidth)/4
    g.drawLine(fract, 0, fract, TankGame.InfoPanelHeight)
    g.drawLine(fract*2, 0, fract*2, TankGame.InfoPanelHeight)
    g.drawLine(fract*3, 0, fract*3, TankGame.InfoPanelHeight)
    
    //drawing lables:
    g.drawString("Health:", 0 + TankGame.InfoPanelPaddings, 10 + TankGame.InfoPanelPaddings)
    g.drawString("Fuel:", fract + TankGame.InfoPanelPaddings, 10 + TankGame.InfoPanelPaddings)
    g.drawString("Ammunition:", fract * 2 + TankGame.InfoPanelPaddings, 10 + TankGame.InfoPanelPaddings)
    g.drawString("Next ammunition:", fract * 3 + TankGame.InfoPanelPaddings, 10 + TankGame.InfoPanelPaddings)
    
    //drawing data:
    val playerTank = world.getTanks.filter {_.id == "Player"}(0)
    g.drawString(playerTank.getHP.toString + "/"+ World.TANKHP.toString , 0 + TankGame.InfoPanelPaddings, 30 + TankGame.InfoPanelPaddings)
    g.drawString(playerTank.getFuelLevel.toString + "/" + World.TANKINITIALFUEL.toString, fract + TankGame.InfoPanelPaddings, 30 + TankGame.InfoPanelPaddings)
    g.drawString(playerTank.getMagazineSize.toString, fract * 2 + TankGame.InfoPanelPaddings, 30 + TankGame.InfoPanelPaddings)
    g.drawString(playerTank.getCurrentAmmunition, fract * 3 + TankGame.InfoPanelPaddings, 30 + TankGame.InfoPanelPaddings)
    
    
    //draws gamefield 
    for( y <- 0 until gamefield.height) {
      for( x <- 0 until gamefield.width) {
        if(gamefield.isWall(x, y)){
          //check around tile if there is not tile on one or more side, draw side tile.
          //also draw those half blocks, check them.
          val img = Images.tileFull
          g.drawImage(img, Help.WorldXToUI(x), Help.WorldYToUI(y), null)
        }
      }
    }
    
    //draws tanks
    world.getTanks.foreach( x => {
      g.drawImage(Images.tank, Help.WorldXToUI(x.vectorPosition.x),
          Help.WorldYToUI(x.vectorPosition.y)+ 3, null)
      drawBarrel(x)
      }
    )
    
    def drawBarrel(tank: Tank): Unit = {
    //draws shoot pointer to gamefield
      val len = TankGame.imageSize /2
      val offset = TankGame.imageSize/2 -1
      val pos = tank.vectorPosition
      val angle =math.Pi*(255 - tank.getCannonAngle)/255 //angle in deg
      val x0 = Help.WorldXToUI(pos.x) + offset
      val y0 = Help.WorldYToUI(pos.y) + offset +3
      val x1 = math.round((x0 + math.cos(angle)* len)).toInt
      val y1 = math.round((y0 - math.sin(angle)* len)).toInt  
      
      g.setColor(Color.BLACK)
      g.drawLine(x0, y0, x1, y1)
    }
    
    def drawPowerIndicator(tank: Tank): Unit = {
      //draws shoot power indicator for tank
      val pos = tank.vectorPosition
      val size = TankGame.imageSize - 4
      val width = TankGame.imageSize/4
      val relativeHeight= tank.getShootPower.toDouble/255
      val height = (TankGame.imageSize*relativeHeight*1.5).toInt
      val x = Help.WorldXToUI(pos.x) + (size*1.2).toInt - width + 6
      val y = Help.WorldYToUI(pos.y) + size - height
      
      g.setColor(new Color((relativeHeight*255).toInt,255-(relativeHeight*255).toInt,0))
      g.fillRect(x, y, width, height)
      g.setColor(Color.BLACK)
      g.drawRect(x,y,width, height)
    }
    
    drawPowerIndicator(playerTank)
    
    //draw bullets
    def drawBullet(bullet: Bullet): Unit = {
      var img = Images.cannonball
      bullet.ammunition match {
        case x:BasicAmmunition => img = Images.cannonball
        case x:HeavyAmmunition => img = Images.missile
      }
      val speed = bullet.getSpeedVector
      val pos = bullet.getPositionVector
      val x = Help.WorldXToUI(pos.x)
      val y = Help.WorldYToUI(pos.y)
      
      g.drawImage(img, x, y, null)
      
    }
    
    if(!world.bulletBuffer.isEmpty) {
      world.bulletBuffer.foreach { drawBullet(_) }
    }
    
    
    //create explosions
    
    if(world.isExpolded()) {
      val pos = world.getExplosionPosition
      this.explosions.append(ExplosionAnimation(pos, TankGame.imageSize*3))
    }
    
    //draw explosions
    def drawExplosions(): Unit = {
      this.explosions.foreach{ x => {
        if(x.active) g.drawImage( x.getNextImage(), Help.WorldXToUI(x.pos.x) - x.size/3, Help.WorldYToUI(x.pos.y) - x.size/3, null)
        }
      }
      val removeBuf = Buffer.empty[Int]
      this.explosions.foreach { x => if(!x.active) removeBuf.append(this.explosions.indexOf(x)) }
      removeBuf.foreach { this.explosions.remove(_)}
    }
    
    drawExplosions
    
  }
  
  
  //timer for requesting updates, this stuff is runned regulary
  
  val timer = Timer(1000/100) {
    world.update(1.0/100)
    this.repaint()
    if(world.endGame) stop()
  }
  
  var StopCounter = 30 //after game has ended animate 10 frames
  def stop(): Unit = {
    if(StopCounter > 0) StopCounter -= 1
    else timer.stop()
  }
  
  timer.start
  
  def playerTank(): Boolean = world.currentTank.id == "Player"
  
  //key listener
  listenTo(keys)
  reactions += {
    case KeyPressed(_, Key.Left,  _, _) => {
      if(playerTank() && world.currentTank.reachedDestination) 
        world.currentTank.moveLeft()
    }
    case KeyPressed(_, Key.Right, _, _) => {
      if(playerTank()&& world.currentTank.reachedDestination)
        world.currentTank.moveRight()
    }
    
    case KeyPressed(_, Key.Up, _, _) => {
      if(playerTank())
        world.currentTank.turnCannonRight(1)
    }
    
    case KeyPressed(_, Key.Down, _, _) => {
      if(playerTank())
        world.currentTank.turnCannonLeft(1)
    }
    case KeyPressed(_, Key.W, _, _) => {
      if(playerTank())
        world.currentTank.increaseShootPower(1)
    }
    case KeyPressed(_, Key.S, _, _) => {
      if(playerTank())
        world.currentTank.decreaseShootPower(1)
    }
    
    case KeyPressed(_, Key.Space, _, _) => {
      if(playerTank()){
        world.currentTank.shoot()
        world.nextTank
      }
    }
  }
  
  focusable = true
  requestFocus
  
  case class ExplosionAnimation(pos: Pos, size: Int) {
    private val images = Vector(Images.loadImage("explosion_1.png", size),
                                Images.loadImage("explosion_2.png", size),
                                Images.loadImage("explosion_3.png", size),
                                Images.loadImage("explosion_4.png", size),
                                Images.loadImage("smoke.png", size))
   private var n = -1
   var active = true
   private var ret = images(0)
   
   def getNextImage(): BufferedImage= {
      n = n + 1
      if(n < images.size*2) {
        ret = images(n/2)
        ret
      }
      else {
        active = false
        ret
      }
    }
  }
  
}
