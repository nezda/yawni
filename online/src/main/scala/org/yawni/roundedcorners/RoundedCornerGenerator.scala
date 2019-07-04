/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.roundedcorners

import javax.imageio.ImageIO
import java.awt._
import java.awt.geom.Arc2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import scala.language.postfixOps

/**
 * Class responsible for bulk of java2d manipulation work when used in the {@link RoundedCornerService}. 
 *
 * This non-standard Firefox CSS rule creates rounded corners:
 * -moz-border-radius:14px 14px 14px 14px !important;
 *
 * This Lift thread discusses using jQuery plugin to add rounded corners:
 *   http://groups.google.com/group/liftweb/browse_frm/thread/37b9732a8a9ba8d1
 *   http://github.com/malsup/corner
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

  private val ANGLE_TOP_LEFT = 90f
  private val ANGLE_TOP_RIGHT = 0f
  private val ANGLE_BOTTOM_LEFT = 180f
  private val ANGLE_BOTTOM_RIGHT = 270f

  private val DEFAULT_FG_COLOR:Color = Color.WHITE
  private val DEFAULT_BG_COLOR:Color = null
  private val DEFAULT_SHADOW_COLOR = ShadowRenderer.DEFAULT_COLOR
  private val DEFAULT_SHADOW_OPACITY = ShadowRenderer.DEFAULT_OPACITY
  private val DEFAULT_ANGLE = ANGLE_TOP_RIGHT

  def buildCorner(fgColor:Option[String], bgColor:Option[String], width:Option[Int], height:Option[Int],
                  angle:Option[String], shadowWidth:Option[Int], endOpacity:Option[Float]): BufferedImage =
    buildCorner(fgColor.map(decodeColor).getOrElse(DEFAULT_FG_COLOR), 
                bgColor.map(decodeColor).getOrElse(DEFAULT_BG_COLOR),
                width.get, height.get, 
                angle.map(getStartAngle).getOrElse(DEFAULT_ANGLE), shadowWidth.get, endOpacity.getOrElse(DEFAULT_SHADOW_OPACITY))

  // issue:
  // if shadowWidth=0
  //   if bgColor == null
  //     background is transparent
  //   * opacity is ignored!?
  // else // shadowWidth > 0
  //   if bgColor == null
  //     * opacity is ignored
  //   else // bgColor != null
  //     opacity seems to apply ONLY to shadow, not remainder of background
  // 
  // * null fgColor should also mean transparent
  //
  // transparency examples here:
  // http://www.apl.jhu.edu/~hall/java/Java2D-Tutorial.html#Java2D-Tutorial-Transparency
  //
  // TODO 
  // - add separate shadow color
  // - 
  private def buildCorner(fgColor:Color, bgColor:Color, width:Int, height:Int,
                          startAngle:Float, shadowWidth:Int, endOpacity:Float): BufferedImage = {
    val msg = "fgColor: "+fgColor+" bgColor: "+bgColor+" width: "+width+" height: "+height+" startAngle: "+startAngle+
      " shadowWidth: "+shadowWidth+" endOpacity: "+endOpacity;
    //System.err.println(msg);

    val w = width * 2
    val h = height * 2

    //FIXME what's this supposed to do ?
    //I think this is simply the case where the corner has no shadow
    if (shadowWidth <= 0) {
      val arc = drawArc(fgColor, w, h, startAngle, false, -1)
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

    val mask = drawArc(fgColor, w, h, startAngle, true, shadowWidth)
    val arc = drawArc(fgColor, w, h, startAngle, false, shadowWidth)

    var startX = 0.0f
    var startY = 0.0f
    val shadowSize = shadowWidth * 2
    val canvasWidth = w + (shadowSize * 2)
    val canvasHeight = h + (shadowSize * 2)

    startAngle match {
      case ANGLE_TOP_LEFT =>
        // no op
      case ANGLE_BOTTOM_LEFT =>
        startY -= shadowSize * 2
      case ANGLE_TOP_RIGHT =>
        startX -= shadowSize * 2
      case ANGLE_BOTTOM_RIGHT =>
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

    val shadow = drawArcShadow(mask, w, h, startAngle, shadowWidth, endOpacity)

    g2.setClip(arcArea)
    g2.drawImage(shadow, 0, 0, null)

    g2.setClip(null)
    g2.drawImage(arc, 0, 0, null)

    convertType(ret, BufferedImage.TYPE_INT_RGB).getSubimage(arcArea.getBounds2D.getX.toInt, arcArea.getBounds2D.getY.toInt,
                                                             arcArea.getBounds2D.getWidth.toInt, arcArea.getBounds2D.getHeight.toInt)
  }

  private def drawArc(arcColor:Color, width:Int, height:Int, angle:Float, masking:Boolean, shadowWidth:Int):BufferedImage = {
    var startAngle = angle
    var canvasWidth = width
    var canvasHeight = height
    var startX = 0
    var startY = 0
    var shadowSize = 0

    if (shadowWidth > 0 && ! masking) {
      shadowSize = shadowWidth * 2
      canvasWidth += shadowSize * 2
      canvasHeight += shadowSize * 2

      startAngle match {
        case ANGLE_TOP_LEFT =>
          startX += shadowSize
          startY += shadowSize
        case ANGLE_BOTTOM_LEFT =>
          startX += shadowSize
          startY -= shadowSize
        case ANGLE_TOP_RIGHT =>
          startX -= shadowSize
          startY += shadowSize
        case ANGLE_BOTTOM_RIGHT =>
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

  private def drawArcShadow(mask:BufferedImage, width:Int, height:Int, 
                            startAngle:Float, shadowWidth:Int, endOpacity:Float): BufferedImage = {
    val shadowSize = shadowWidth * 2
    var sampleY = 0
    var sampleX = 0
    var sampleWidth = width + shadowSize
    var sampleHeight = height + shadowSize

    startAngle match {
      case ANGLE_TOP_LEFT =>
        // no op
      case ANGLE_BOTTOM_LEFT =>
        sampleWidth -= shadowSize
        sampleHeight = height

        sampleY += shadowSize
      case ANGLE_TOP_RIGHT =>
        sampleWidth -= shadowSize
        sampleHeight -= shadowSize

        sampleX += shadowSize
      case ANGLE_BOTTOM_RIGHT =>
        sampleWidth -= shadowSize
        sampleHeight -= shadowSize

        sampleX += shadowSize
        sampleY += shadowSize
    }

    val shadowRenderer = ShadowRenderer(shadowWidth, endOpacity, DEFAULT_SHADOW_COLOR)
    val dropShadow = shadowRenderer.createShadow(mask)

    // draw shadow arc

    val img = new BufferedImage(width * 4, height * 4, BufferedImage.TYPE_INT_ARGB)
    val g2:Graphics2D = img.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setComposite(AlphaComposite.Src)
    g2.drawImage(dropShadow, 0, 0, null)

    g2.dispose()

    img.getSubimage(sampleX, sampleY, sampleWidth, sampleHeight)
  }

  def buildShadow(fgColor:Option[String], bgColor:Option[String], width:Option[Int], height:Option[Int],
                  arcWidth:Option[Float], arcHeight:Option[Float],
                  shadowWidth:Option[Int], endOpacity:Option[Float]): BufferedImage =
    buildShadow(fgColor.map(decodeColor).getOrElse(DEFAULT_FG_COLOR), 
                bgColor.map(decodeColor).getOrElse(DEFAULT_BG_COLOR),
                width.get, height.get, arcWidth.get, arcHeight.get, 
                shadowWidth.get, endOpacity.getOrElse(DEFAULT_SHADOW_OPACITY))

  private def buildShadow(fgColor:Color, bgColor:Color, width:Int, height:Int, arcWidth:Float, arcHeight:Float,
                          shadowWidth:Int, endOpacity:Float): BufferedImage = {
    val mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    var g2 = mask.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    var fillArea = new RoundRectangle2D.Float(0, 0, width, height, arcHeight, arcWidth)
    g2.setColor(fgColor)
    g2.fill(fillArea)
    g2.dispose()

    // clip shadow

    val shadowRenderer = ShadowRenderer(shadowWidth, endOpacity, DEFAULT_SHADOW_COLOR)
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

  def buildSideShadow(side:Option[String], size:Option[Int], opacity:Option[Float]): BufferedImage =
    buildSideShadow(side.get, size.get, opacity.getOrElse(DEFAULT_SHADOW_OPACITY))

  private def buildSideShadow(side:String, size:Int, opacity:Float): BufferedImage = {
    require(opacity >= 0)

    var maskWidth = 0
    var maskHeight = 0
    var sampleY = 0
    var sampleX = 0
    var sampleWidth = 0
    var sampleHeight = 0

    side match {
      case LEFT =>
        maskWidth = size * 4
        maskHeight = size * 4
        sampleY = maskHeight / 2
        sampleWidth = size * 2
        sampleHeight = 2
      case RIGHT =>
        maskWidth = size * 4
        maskHeight = size * 4
        sampleY = maskHeight / 2
        sampleX = maskWidth
        sampleWidth = size * 2
        sampleHeight = 2
      case BOTTOM =>
        maskWidth = size * 4
        maskHeight = size * 4
        sampleY = maskHeight
        sampleX = maskWidth / 2
        sampleWidth = 2
        sampleHeight = size * 2
      case TOP =>
        maskWidth = size * 4
        maskHeight = size * 4
        sampleY = 0
        sampleX = maskWidth / 2
        sampleWidth = 2
        sampleHeight = size * 2
    }

    val mask = new BufferedImage(maskWidth, maskHeight, BufferedImage.TYPE_INT_ARGB)
    var g2:Graphics2D = mask.createGraphics()

    g2.setColor(Color.WHITE)
    g2.fillRect(0, 0, maskWidth, maskHeight)

    g2.dispose()

    val shadowRenderer = ShadowRenderer(size, opacity, DEFAULT_SHADOW_COLOR)
    val dropShadow = shadowRenderer.createShadow(mask)

    val render = new BufferedImage(maskWidth * 2, maskHeight * 2, BufferedImage.TYPE_INT_ARGB)
    g2 = render.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val clip = new Rectangle2D.Float(sampleX, sampleY, sampleWidth, sampleHeight)

    g2.setColor(Color.WHITE)
    g2.fill(clip)

    g2.drawImage(dropShadow, 0, 0, null)

    g2.dispose()

    render.getSubimage(sampleX, sampleY, sampleWidth, sampleHeight)
  }

  /**
   * Matches the incoming string against one of the defined constants; "tl", "tr", "bl", "br"
   * @param code The code for the angle of the arc to generate, if no match is found the default is
   *          {@link #TOP_RIGHT} - or 0 degrees.
   * @return The pre-defined 90 degree angle starting degree point.
   */
  private def getStartAngle(code:String):Float = code.toLowerCase match {
    case TOP_LEFT => ANGLE_TOP_LEFT
    case TOP_RIGHT => ANGLE_TOP_RIGHT
    case BOTTOM_LEFT => ANGLE_BOTTOM_LEFT
    case BOTTOM_RIGHT => ANGLE_BOTTOM_RIGHT
  }

  /** 
   * CSS2 color spec <a href="http://www.w3.org/TR/REC-CSS2/syndata.html#color-units">
   *   http://www.w3.org/TR/REC-CSS2/syndata.html#color-units</a>
   */
  private val cssSpecMap = Map(
    "maroon" -> new Color(128,0,0),
    "red" -> Color.RED,
    "orange" -> Color.ORANGE,
    "yellow" -> Color.YELLOW,
    "olive" -> new Color(128,128,0),
    "purple" -> new Color(128,0,128),
    "fuchsia" -> new Color(255,0,255),
    "white" -> Color.WHITE,
    "lime" -> new Color(0,255,0),
    "green" -> Color.GREEN,
    "navy" -> new Color(0,0,128),
    "blue" -> Color.BLUE,
    "aqua" -> new Color(0,255,255),
    "teal" -> new Color(0,128,128),
    "black" -> Color.BLACK,
    "silver" -> new Color(192,192,192),
    "gray" -> Color.GRAY
  )

  private val ColorRE = """0x.+"""r

  /**
   * Decodes the specified input color string into a compatible awt color object. Valid inputs
   * are any in the CSS2 color spec or hex strings.
   * @param color The color to match.
   * @return The decoded color object, may be black if decoding fails.
   */
  private def decodeColor(code:String):Color = cssSpecMap.get(code) match {
    case Some(color) => color
    case None => code match {
      case ColorRE(code) => Color.decode(code)
      case _ => Color.decode("0x"+code)
    }
  }

  private def convertType(image:BufferedImage, imageType:Int):BufferedImage = {
    val result = new BufferedImage(image.getWidth(), image.getHeight(), imageType)
    val g = result.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawRenderedImage(image, null)
    g.dispose()
    result
  }

}
