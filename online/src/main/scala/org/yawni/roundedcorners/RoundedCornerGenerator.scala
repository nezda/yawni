//package org.apache.tapestry.contrib.services.impl
package org.yawni.roundedcorners

import javax.imageio.ImageIO
import java.awt._
import java.awt.geom.Arc2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * Class responsible for bulk of java2d manipulation work when used in the {@link RoundedCornerService}. 
 */
object RoundedCornerGenerator {
  private val TOP_LEFT = "tl"
  private val TOP_RIGHT = "tr"
  private val BOTTOM_LEFT = "bl"
  private val BOTTOM_RIGHT = "br"

  private val LEFT = "left"
  private val RIGHT = "right"
  private val TOP = "top"
  private val BOTTOM = "bottom"
  ImageIO.setUseCache(false)

  // css2 color spec - http://www.w3.org/TR/REC-CSS2/syndata.html#color-units
  private val cssSpecMap = Map(
    "aqua" -> new Color(0,255,255),
    "black" -> Color.BLACK,
    "blue" -> Color.BLUE,
    "fuchsia" -> new Color(255,0,255),
    "gray" -> Color.GRAY,
    "green" -> Color.GREEN,
    "lime" -> new Color(0,255,0),
    "maroon" -> new Color(128,0,0),
    "navy" -> new Color(0,0,128),
    "olive" -> new Color(128,128,0),
    "purple" -> new Color(128,0,128),
    "red" -> Color.RED,
    "silver" -> new Color(192,192,192),
    "teal" -> new Color(0,128,128),
    "white" -> Color.WHITE,
    "yellow" -> Color.YELLOW
  )

  private val SHADOW_COLOR = new Color(0x000000)

  private val DEFAULT_OPACITY = 0.5f

  private val ANGLE_TOP_LEFT = 90f
  private val ANGLE_TOP_RIGHT = 0f
  private val ANGLE_BOTTOM_LEFT = 180f
  private val ANGLE_BOTTOM_RIGHT = 270f

  def buildCorner(color:String, backgroundColor:String, width:Int, height:Int,
                  angle:String, shadowWidth:Int, endOpacity:Float): BufferedImage = {
    val w = width * 2
    val h = height * 2
    val startAngle = getStartAngle(angle)
    val bgColor:Color = if (backgroundColor == null) null else decodeColor(backgroundColor)

    if (shadowWidth <= 0) {
      val arc = drawArc(color, w, h, angle, false, -1)
      var ret = arc

      val arcArea = new Arc2D.Float(0, 0, w, h, startAngle, 90, Arc2D.PIE)
      if (bgColor != null) {
        ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2: Graphics2D = ret.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        g2.setColor(bgColor)
        g2.fill(arcArea.getBounds2D())

        g2.drawImage(arc, 0, 0, null)

        g2.dispose()

        ret = convertType(ret, BufferedImage.TYPE_INT_RGB)
      }

      return ret.getSubimage(arcArea.getBounds2D.getX.toInt, arcArea.getBounds2D.getY.toInt,
                             arcArea.getBounds2D.getWidth.toInt, arcArea.getBounds2D.getHeight.toInt)
    }

    val mask = drawArc(color, w, h, angle, true, shadowWidth)
    val arc = drawArc(color, w, h, angle, false, shadowWidth)

    var startX = 00.f
    var startY = 0.0f
    val shadowSize = shadowWidth * 2
    val canvasWidth = w + (shadowSize * 2)
    val canvasHeight = h + (shadowSize * 2)

    if (startAngle == ANGLE_BOTTOM_LEFT) {
      startY -= shadowSize * 2
    } else if (startAngle == ANGLE_TOP_RIGHT) {
      startX -= shadowSize * 2
    } else if (startAngle == ANGLE_BOTTOM_RIGHT) {
      startX -= shadowSize * 2
      startY -= shadowSize * 2
    }

    val ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2: Graphics2D = ret.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val arcArea = new Arc2D.Float(startX, startY, canvasWidth, canvasHeight, startAngle, 90, Arc2D.PIE)

    if (bgColor != null) {
      g2.setColor(bgColor)
      g2.fill(arcArea.getBounds2D())
    }

    val shadow = drawArcShadow(mask, color, backgroundColor, w, h, angle, shadowWidth, endOpacity)

    g2.setClip(arcArea)
    g2.drawImage(shadow, 0, 0, null)

    g2.setClip(null)
    g2.drawImage(arc, 0, 0, null)

    convertType(ret, BufferedImage.TYPE_INT_RGB).getSubimage(arcArea.getBounds2D.getX.toInt, arcArea.getBounds2D.getY.toInt,
                                                             arcArea.getBounds2D.getWidth.toInt, arcArea.getBounds2D.getHeight.toInt)
  }

  def convertType(image:BufferedImage, imageType:Int):BufferedImage = {
    val result = new BufferedImage(image.getWidth(), image.getHeight(), imageType)
    val g = result.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawRenderedImage(image, null)
    g.dispose()
    result
  }

  def drawArc(color:String, width:Int, height:Int, angle:String, masking:Boolean, shadowWidth:Int):BufferedImage = {
    val arcColor = decodeColor(color)
    var startAngle = getStartAngle(angle)

    var canvasWidth = width
    var canvasHeight = height
    var startX = 0
    var startY = 0
    var shadowSize = 0

    if (shadowWidth > 0 && ! masking) {
      shadowSize = shadowWidth * 2
      canvasWidth += shadowSize * 2
      canvasHeight += shadowSize * 2

      if (startAngle == ANGLE_TOP_LEFT) {
        startX += shadowSize
        startY += shadowSize
      } else if (startAngle == ANGLE_BOTTOM_LEFT) {
        startX += shadowSize
        startY -= shadowSize
      } else if (startAngle == ANGLE_TOP_RIGHT) {
        startX -= shadowSize
        startY += shadowSize
      } else if (startAngle == ANGLE_BOTTOM_RIGHT) {
        startX -= shadowSize
        startY -= shadowSize
      }
    }

    val img = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB)
    val g2:Graphics2D = img.createGraphics()

    var extent = 90.0f
    if (masking) {
      extent = 120
      startAngle -= 20
    }

    val fillArea = new Arc2D.Float(startX, startY, width, height, startAngle, extent, Arc2D.PIE)

    // draw arc
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

    g2.setColor(arcColor)
    g2.setComposite(AlphaComposite.Src)
    g2.fill(fillArea)

    g2.dispose()

    img
  }

  def drawArcShadow(mask:BufferedImage, color:String, backgroundColor:String, width:Int, height:Int,
                    angle:String, shadowWidth:Int, endOpacity:Float): BufferedImage = {
    val startAngle = getStartAngle(angle)
    val shadowSize = shadowWidth * 2
    var sampleY = 0
    var sampleX = 0
    var sampleWidth = width + shadowSize
    var sampleHeight = height + shadowSize

    if (startAngle == ANGLE_TOP_LEFT) {
      // no op
    } else if (startAngle == ANGLE_BOTTOM_LEFT) {
      sampleWidth -= shadowSize
      sampleHeight = height

      sampleY += shadowSize
    } else if (startAngle == ANGLE_TOP_RIGHT) {
      sampleWidth -= shadowSize
      sampleHeight -= shadowSize

      sampleX += shadowSize
    } else if (startAngle == ANGLE_BOTTOM_RIGHT) {
      sampleWidth -= shadowSize
      sampleHeight -= shadowSize

      sampleX += shadowSize
      sampleY += shadowSize
    }

    val shadowRenderer = new ShadowRenderer(shadowWidth, endOpacity, SHADOW_COLOR)
    val dropShadow = shadowRenderer.createShadow(mask)

    // draw shadow arc

    val img = new BufferedImage((width * 4), (height * 4), BufferedImage.TYPE_INT_ARGB)
    val g2:Graphics2D = img.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setComposite(AlphaComposite.Src)
    g2.drawImage(dropShadow, 0, 0, null)

    g2.dispose()

    img.getSubimage(sampleX, sampleY, sampleWidth, sampleHeight)
  }

  def buildShadow(color:String, backgroundColor:String, width:Int, height:Int,
                  arcWidth:Float, arcHeight:Float,
                  shadowWidth:Int, endOpacity:Float): BufferedImage = {
    val fgColor = if (color == null) Color.WHITE else decodeColor(color)
    val bgColor:Color = if (backgroundColor == null) null else decodeColor(backgroundColor)

    val mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    var g2 = mask.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    var fillArea = new RoundRectangle2D.Float(0, 0, width, height, arcHeight, arcWidth)
    g2.setColor(fgColor)
    g2.fill(fillArea)
    g2.dispose()

    // clip shadow

    val shadowRenderer = new ShadowRenderer(shadowWidth, endOpacity, SHADOW_COLOR)
    val dropShadow = shadowRenderer.createShadow(mask)

    val clipImg = new BufferedImage(width + (shadowWidth * 2), height + (shadowWidth * 2), BufferedImage.TYPE_INT_ARGB)
    g2 = clipImg.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setComposite(AlphaComposite.Src)

    val clip = new RoundRectangle2D.Float(0, 0, width + (shadowWidth * 2), height + (shadowWidth * 2), arcHeight, arcWidth)
    g2.setClip(clip)
    g2.drawImage(dropShadow, 0, 0, null)
    g2.dispose()

    // draw everything

    val img = new BufferedImage(width + (shadowWidth * 2), height + (shadowWidth * 2), BufferedImage.TYPE_INT_ARGB)
    g2 = img.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    if (bgColor != null) {
      fillArea = new RoundRectangle2D.Float(0, 0, width + (shadowWidth * 2), height + (shadowWidth * 2), arcHeight, arcWidth)
      g2.setColor(bgColor)
      g2.fill(fillArea.getBounds2D())
    }

    g2.drawImage(clipImg, 0, 0, null)

    if (fgColor != null) {
      fillArea = new RoundRectangle2D.Float(0, 0, width, height, arcHeight, arcWidth)
      g2.setColor(fgColor)
      g2.fill(fillArea)
    }

    g2.dispose()

    convertType(img, BufferedImage.TYPE_INT_RGB)
  }

  def buildSideShadow(side:String, size:Int, opacity:Float): BufferedImage = {
    if (opacity <= 0)
      throw new IllegalArgumentException("opacity <= 0")

    var maskWidth = 0
    var maskHeight = 0
    var sampleY = 0
    var sampleX = 0
    var sampleWidth = 0
    var sampleHeight = 0

    if (LEFT.equals(side)) {
      maskWidth = size * 4
      maskHeight = size * 4
      sampleY = maskHeight / 2
      sampleWidth = size * 2
      sampleHeight = 2
    } else if (RIGHT.equals(side)) {
      maskWidth = size * 4
      maskHeight = size * 4
      sampleY = maskHeight / 2
      sampleX = maskWidth
      sampleWidth = size * 2
      sampleHeight = 2
    } else if (BOTTOM.equals(side)) {
      maskWidth = size * 4
      maskHeight = size * 4
      sampleY = maskHeight
      sampleX = maskWidth / 2
      sampleWidth = 2
      sampleHeight = size * 2
    } else if (TOP.equals(side)) {
      maskWidth = size * 4
      maskHeight = size * 4
      sampleY = 0
      sampleX = maskWidth / 2
      sampleWidth = 2
      sampleHeight = size * 2
    }

    val mask = new BufferedImage(maskWidth, maskHeight, BufferedImage.TYPE_INT_ARGB)
    var g2:Graphics2D = mask.createGraphics()

    g2.setColor(Color.white)
    g2.fillRect(0, 0, maskWidth, maskHeight)

    g2.dispose()

    val shadowRenderer = new ShadowRenderer(size, opacity, SHADOW_COLOR)
    val dropShadow = shadowRenderer.createShadow(mask)

    val render = new BufferedImage(maskWidth * 2, maskHeight * 2, BufferedImage.TYPE_INT_ARGB)
    g2 = render.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val clip = new Rectangle2D.Float(sampleX, sampleY, sampleWidth, sampleHeight)

    g2.setColor(Color.white)
    g2.fill(clip)

    g2.drawImage(dropShadow, 0, 0, null)

    g2.dispose()

    render.getSubimage(sampleX, sampleY, sampleWidth, sampleHeight)
  }

  /**
   * Matches the incoming string against one of the constants defined; tl, tr, bl, br.
   * @param code The code for the angle of the arc to generate, if no match is found the default is
   *          {@link #TOP_RIGHT} - or 0 degrees.
   * @return The pre-defined 90 degree angle starting degree point.
   */
  def getStartAngle(code:String):Float = {
    if (TOP_LEFT.equalsIgnoreCase(code))
      return ANGLE_TOP_LEFT
    if (TOP_RIGHT.equalsIgnoreCase(code))
      return ANGLE_TOP_RIGHT
    if (BOTTOM_LEFT.equalsIgnoreCase(code))
      return ANGLE_BOTTOM_LEFT
    if (BOTTOM_RIGHT.equalsIgnoreCase(code))
      return ANGLE_BOTTOM_RIGHT
    return ANGLE_TOP_RIGHT
  }

  /**
   * Decodes the specified input color string into a compatible awt color object. Valid inputs
   * are any in the css2 color spec or hex strings.
   * @param color The color to match.
   * @return The decoded color object, may be black if decoding fails.
   */
  def decodeColor(color:String):Color = {
    val specColor = cssSpecMap.getOrElse(color, null)
    if (specColor != null)
      return specColor
    val hexColor = if (color.startsWith("0x")) color else "0x" + color
    return Color.decode(hexColor)
  }
}
