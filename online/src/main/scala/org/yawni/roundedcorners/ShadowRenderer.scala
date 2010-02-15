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

import java.awt._
import java.awt.image.BufferedImage
import java.awt.image.Raster
import java.awt.image.WritableRaster

/**
 * A shadow renderer needs three properties to generate shadows: size, opacity, and color
 * @param size the size of the shadow in pixels. Defines the blur radius to create the fuzziness.
 * @param opacity the opacity of the shadow; between 0.0 (totally transparent) and 1.0 (fully opaque)
 * @param color the color of the shadow; {@link Color.BLACK} common, but not mandatory
 */
case class ShadowRenderer(size:Int, 
                          opacity:Float, 
                          color:Color) {
  require(size >= 0)
  require(color != null)
  require(opacity >= 0.0f && opacity <= 1.0f)

  def this(size:Int, opacity:Float) = this(size, opacity, ShadowRenderer.DEFAULT_COLOR)
  def this(size:Int) = this(size, ShadowRenderer.DEFAULT_OPACITY)
  def this() = this(ShadowRenderer.DEFAULT_SIZE)

  /**
   * Generates the shadow for a given picture using the current properties
   * of the renderer.
   * <p>The generated image dimensions are computed as follows:</p>
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
    //XXX for (int srcY = 0, dstOffset = left * dstWidth; srcY < srcHeight; srcY++)
    var dstOffset = left * dstWidth
    for {srcY <- 0 until srcHeight} {
      // first pixels are empty
      for {i <- 0 until shadowSize} {
        aHistory(i) = 0
      }

      aSum = 0
      historyIdx = 0
      srcOffset = srcY * srcWidth

      // compute the blur average with pixels from the source image
      for {srcX <- 0 until srcWidth} {
        var a = hSumLookup(aSum)
        dstBuffer(dstOffset) = a << 24   // store the alpha value only
        dstOffset += 1                   // the shadow color will be added in the next pass
        
        aSum -= aHistory(historyIdx)     // subtract the oldest pixel from the sum

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

        // subtract the oldest pixel from the sum ... and nothing new to add !
        aSum -= aHistory(historyIdx)

        historyIdx += 1
        if (historyIdx >= shadowSize) {
          historyIdx -= shadowSize
        }
      }
    }

    // vertical pass
    var bufferOffset = 0
    for {x <- 0 until dstWidth} {
      bufferOffset = x
      aSum = 0

      // first pixels are empty
      for {i <- 0 until left} {
        aHistory(i) = 0
      }
      historyIdx = left

      // and then they come from the dstBuffer
      //XXX for (int y = 0 y < right; y++, bufferOffset += dstWidth)
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
      //XXX for (int y = 0; y < yStop; y++, bufferOffset += dstWidth)
      for {y <- 0 until yStop} {
        var a = vSumLookup(aSum)
        dstBuffer(bufferOffset) = a << 24 | shadowRgb  // store alpha value + shadow color

        aSum -= aHistory(historyIdx)   // subtract the oldest pixel from the sum
        
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
      //XXX for (int y = yStop y < dstHeight; y++, bufferOffset += dstWidth)
      for {y <- yStop until dstHeight} {
        val a = vSumLookup(aSum)
        dstBuffer(bufferOffset) = a << 24 | shadowRgb

        aSum -= aHistory(historyIdx)   // subtract the oldest pixel from the sum

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
    require(w != 0)
    require(h != 0)
    //if (w == 0 || h == 0) {
    //  //return Array(0)
    //  throw new IllegalArgumentException("w: " + w + " h: " + h);
    //}

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

object ShadowRenderer {
  val DEFAULT_SIZE = 5
  val DEFAULT_COLOR = Color.BLACK
  val DEFAULT_OPACITY = 0.5f
}
