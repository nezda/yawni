package org.yawni.roundedcorners

import java.awt._
import java.awt.image.BufferedImage
import java.awt.image.Raster
import java.awt.image.WritableRaster

/**
 * A shadow renderer needs three properties to generate shadows.
 * These properties are:
 * <ul>
 *   <li><i>size</i>: The size, in pixels, of the shadow. This property also
 *   defines the fuzzyness.</li>
 *   <li><i>opacity</i>: The opacity, between 0.0 and 1.0, of the shadow.</li>
 *   <li><i>color</i>: The color of the shadow. Shadows are not meant to be
 *   black only.</li>
 * </ul>
 * @param size the size of the shadow in pixels. Defines the fuzziness.
 * @param opacity the opacity of the shadow.
 * @param color the color of the shadow.
 */
class ShadowRenderer(private var size:Int, 
                     private var opacity:Float, 
                     private var color:Color) {
  setSize(size)
  setOpacity(opacity)
  setColor(color)

  // size of the shadow in pixels (defines the fuzziness)
  //private val size = 5

  // opacity of the shadow
  //private val opacity = 0.5f
  
  // color of the shadow
  //private val color = Color.BLACK

  /**
   * Gets the color used by the renderer to generate shadows.
   * @return this renderer's shadow color
   */
  def getColor() {
    color
  }

  /**
   * Sets the color used by the renderer to generate shadows.
   * <p>Consecutive calls to {@link #createShadow} will all use this color
   * until it is set again.</p>
   * <p>If the color provided is null, the previous color will be retained.</p>
   * @param shadowColor the generated shadows color
   */
  private def setColor(shadowColor:Color) {
    if (shadowColor != null) {
      val oldColor = this.color
      this.color = shadowColor
    }
  }

  /**
   * Gets the opacity used by the renderer to generate shadows.
   * <p>The opacity is comprised between 0.0f and 1.0f; 0.0f being fully
   * transparent and 1.0f fully opaque.</p>
   * @return this renderer's shadow opacity
   */
  def getOpacity() {
    opacity
  }

  /**
   * Sets the opacity used by the renderer to generate shadows.
   * <p>Consecutive calls to {@link #createShadow} will all use this opacity
   * until it is set again.</p>
   * <p>The opacity is comprised between 0.0f and 1.0f; 0.0f being fully
   * transparent and 1.0f fully opaque. If you provide a value out of these
   * boundaries, it will be restrained to the closest boundary.</p>
   * @param shadowOpacity the generated shadows opacity
   */
  private def setOpacity(shadowOpacity:Float) {
    val oldOpacity = this.opacity
    if (shadowOpacity < 0.0) {
      this.opacity = 0.0f
    } else if (shadowOpacity > 1.0f) {
      this.opacity = 1.0f
    } else {
      this.opacity = shadowOpacity
    }
  }

  /**
   * Gets the size in pixel used by the renderer to generate shadows.
   * @return this renderer's shadow size
   */
  def getSize() {
    return size
  }

  /**
   * Sets the size, in pixels, used by the renderer to generate shadows.
   * <p>The size defines the blur radius applied to the shadow to create the
   * fuzziness.</p>
   * <p>There is virtually no limit to the size. The size cannot be negative.
   * If you provide a negative value, the size will be 0 instead.</p>
   * @param shadowSize the generated shadows size in pixels (fuzziness)
   */
  private def setSize(shadowSize:Int) {
    val oldSize = this.size
    if (shadowSize < 0) {
      this.size = 0
    } else {
      this.size = shadowSize
    }
  }

  /**
   * Generates the shadow for a given picture and the current properties
   * of the renderer.
   * <p>The generated image dimensions are computed as following:</p>
   * <pre>{@code
   * width  = imageWidth  + 2 * shadowSize
   * height = imageHeight + 2 * shadowSize
   * }</pre>
   * @param image the picture from which the shadow must be cast
   * @return the picture containing the shadow of {@code image}
   */
  def createShadow(image:BufferedImage):BufferedImage = {
    // Written by Sesbastien Petrucci
    val shadowSize = size * 2

    val srcWidth = image.getWidth
    val srcHeight = image.getHeight

    val dstWidth = srcWidth + shadowSize
    val dstHeight = srcHeight + shadowSize

    val left = size
    val right = shadowSize - left

    val yStop = dstHeight - right

    val shadowRgb = color.getRGB() & 0x00FFFFFF
    val aHistory = new Array[Int](shadowSize)
    var historyIdx = 0

    var aSum = 0

    val dst = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_ARGB)
    val dstBuffer = new Array[Int](dstWidth * dstHeight)
    val srcBuffer = new Array[Int](srcWidth * srcHeight)

    getPixels(image, 0, 0, srcWidth, srcHeight, srcBuffer)

    val lastPixelOffset = right * dstWidth
    val hSumDivider = 1.0f / shadowSize
    val vSumDivider = opacity / shadowSize

    val hSumLookup = new Array[Int](256 * shadowSize)
    for {i <- 0 until hSumLookup.length} {
      hSumLookup(i) = (i * hSumDivider).toInt
    }

    val vSumLookup = new Array[Int](256 * shadowSize)
    for {i <- 0 until vSumLookup.length} {
      vSumLookup(i) = (i * vSumDivider).toInt
    }

    var srcOffset = 0

    // horizontal pass : extract the alpha mask from the source picture and
    // blur it into the destination picture
    //XXX for (int srcY = 0, dstOffset = left * dstWidth; srcY < srcHeight; srcY++) {
    var dstOffset = left * dstWidth
    for {srcY <- 0 until srcHeight} {
      // first pixels are empty
      for {historyIdx <- 0 until shadowSize} {
        aHistory(historyIdx) = 0
      }

      aSum = 0
      historyIdx = 0
      srcOffset = srcY * srcWidth

      // compute the blur average with pixels from the source image
      for {srcX <- 0 until srcWidth} {
        var a = hSumLookup(aSum)
        dstBuffer(dstOffset) = a << 24   // store the alpha value only
        dstOffset += 1                   // the shadow color will be added in the next pass
        
        aSum -= aHistory(historyIdx) // substract the oldest pixel from the sum

        // extract the new pixel ...
        a = srcBuffer(srcOffset + srcX) >>> 24
        aHistory(historyIdx) = a   // ... and store its value into history
        aSum += a                  // ... and add its value to the sum

        historyIdx += 1
        if (historyIdx >= shadowSize) {
          historyIdx -= shadowSize
        }
      }

      // blur the end of the row - no new pixels to grab
      for {i <- 0 until shadowSize} {
        val a = hSumLookup(aSum)
        dstBuffer(dstOffset) = a << 24
        dstOffset += 1

        // substract the oldest pixel from the sum ... and nothing new to add !
        aSum -= aHistory(historyIdx)

        historyIdx += 1
        if (historyIdx >= shadowSize) {
          historyIdx -= shadowSize
        }
      }
    }

    // vertical pass
    //XXX for (var x <- 0 to dstWidth; bufferOffset <- 0 to ; x < dstWidth; x++, bufferOffset = x) {
    //for (int x = 0, bufferOffset = 0; x < dstWidth; x++, bufferOffset = x) {
    var bufferOffset = 0
    for {x <- 0 until dstWidth} {
      bufferOffset = x
      aSum = 0

      // first pixels are empty
      for {historyIdx <- 0 until left} {
        aHistory(historyIdx) = 0
      }

      // and then they come from the dstBuffer
      //XXX for (int y = 0 y < right; y++, bufferOffset += dstWidth) {
      for {y <- 0 until right} {
        val a = dstBuffer(bufferOffset) >>> 24         // extract alpha
        aHistory(historyIdx) = a                       // store into history
        aSum += a                                      // and add to sum
        historyIdx += 1
        bufferOffset += dstWidth
      }

      bufferOffset = x
      historyIdx = 0

      // compute the blur average with pixels from the previous pass
      //XXX for (int y = 0 y < yStop; y++, bufferOffset += dstWidth) {
      for {y <- 0 until yStop} {
        var a = vSumLookup(aSum)
        dstBuffer(bufferOffset) = a << 24 | shadowRgb  // store alpha value + shadow color

        aSum -= aHistory(historyIdx)   // substract the oldest pixel from the sum
        
        a = dstBuffer(bufferOffset + lastPixelOffset) >>> 24   // extract the new pixel ...
        aHistory(historyIdx) = a                               // ... and store its value into history
        aSum += a                                              // ... and add its value to the sum

        historyIdx += 1
        if (historyIdx >= shadowSize) {
          historyIdx -= shadowSize
        }
        bufferOffset += dstWidth
      }

      // blur the end of the column - no pixels to grab anymore
      //XXX for (int y = yStop y < dstHeight; y++, bufferOffset += dstWidth) {
      for {y <- yStop until dstHeight} {
        val a = vSumLookup(aSum)
        dstBuffer(bufferOffset) = a << 24 | shadowRgb

        aSum -= aHistory(historyIdx)   // substract the oldest pixel from the sum

        historyIdx += 1
        if (historyIdx >= shadowSize) {
          historyIdx -= shadowSize
        }
        bufferOffset += dstWidth
      }
    }

    setPixels(dst, 0, 0, dstWidth, dstHeight, dstBuffer)
    dst
  }

  /**
   * Returns an array of pixels, stored as integers, from a
   * {@code BufferedImage}. The pixels are grabbed from a rectangular
   * area defined by a location and two dimensions. Calling this method on
   * an image of type different from {@code BufferedImage.TYPE_INT_ARGB}
   * and {@code BufferedImage.TYPE_INT_RGB} will unmanage the image.
   * @param img the source image
   * @param x the x location at which to start grabbing pixels
   * @param y the y location at which to start grabbing pixels
   * @param w the width of the rectangle of pixels to grab
   * @param h the height of the rectangle of pixels to grab
   * @param pixels a pre-allocated array of pixels of size w*h; can be null
   * @return {@code pixels} if non-null, a new array of integers
   *   otherwise
   * @throws IllegalArgumentException is {@code pixels} is non-null and
   *   of length &lt; w*h
   */
  private def getPixels(img:BufferedImage, x:Int, y:Int, w:Int, h:Int, pxls:Array[Int]) {
    if (w == 0 || h == 0) {
      //return Array(0)
      throw new IllegalArgumentException("w: "+w+" h: "+h);
    }

    val pixels = 
      if (pxls == null) {
        new Array[Int](w * h)
      } else if (pxls.length < w * h) {
        throw new IllegalArgumentException("pixels array must have a length >= w*h")
      } else {
        pxls
      }

    val imageType = img.getType()
    if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
      val raster = img.getRaster()
      return raster.getDataElements(x, y, w, h, pixels)
    }

    // Unmanages the image
    img.getRGB(x, y, w, h, pixels, 0, w)
  }

  /**
   * Writes a rectangular area of pixels in the destination
   * {@code BufferedImage}. Calling this method on
   * an image of type different from {@code BufferedImage.TYPE_INT_ARGB}
   * and {@code BufferedImage.TYPE_INT_RGB} will unmanage the image.
   *
   * @param img the destination image
   * @param x the x location at which to start storing pixels
   * @param y the y location at which to start storing pixels
   * @param w the width of the rectangle of pixels to store
   * @param h the height of the rectangle of pixels to store
   * @param pixels an array of pixels, stored as integers
   * @throws IllegalArgumentException is {@code pixels} is non-null and
   *   of length &lt; w*h
   */
  private def setPixels(img:BufferedImage, x:Int, y:Int, w:Int, h:Int, pixels:Array[Int]) {
    if (pixels == null || w == 0 || h == 0) {
      return
    } else if (pixels.length < w * h) {
      throw new IllegalArgumentException("pixels array must have a length >= w*h")
    }

    val imageType = img.getType
    if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
      val raster: WritableRaster  = img.getRaster()
      raster.setDataElements(x, y, w, h, pixels)
    } else {
      // Unmanages the image
      img.setRGB(x, y, w, h, pixels, 0, w)
    }
  }
}
