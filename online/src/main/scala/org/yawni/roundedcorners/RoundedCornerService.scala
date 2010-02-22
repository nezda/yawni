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
package org.yawni.wordnet.snippet

import scala.xml.{ Text, NodeSeq }
import net.liftweb.http.{ S, SHtml, XmlResponse }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers._

import net.liftweb.http.{ LiftResponse, Req, GetRequest, PostRequest, LiftRules, JsonResponse, PlainTextResponse, InMemoryResponse }
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JE._

import org.yawni.roundedcorners._
import java.io._
import javax.imageio._

/**
 * Well documented <a href="http://tapestry.apache.org/tapestry4.1/developmentguide/hivemind/roundedcorners.html">
 *   http://tapestry.apache.org/tapestry4.1/developmentguide/hivemind/roundedcorners.html</a>
 * Provides generated rounded corner images in a similar use / fashion as
 * outlined here: <a href="http://xach.livejournal.com/95656.html">google's own cornershop</a>.
 * Still online supported: <a href="http://groups.google.com/groups/roundedcorners?c=999999&bc=white&w=60&h=60&a=tr">
 *   http://groups.google.com/groups/roundedcorners?c=999999&bc=white&w=60&h=60&a=tr</a>
 */
// test URLs
// http://localhost:8080/rounded?c=FF9900&bc=white&w=60&h=60&a=tr&sw=3&o=.5
object RoundedCornerService {
  private val SERVICE_NAME = "rounded"
  private val PARM_COLOR = "c"
  private val PARM_BACKGROUND_COLOR = "bc"
  private val PARM_WIDTH = "w"
  private val PARM_HEIGHT = "h"
  private val PARM_ANGLE = "a"
  private val PARM_SHADOW_WIDTH ="sw"
  private val PARM_SHADOW_OPACITY ="o"
  private val PARM_SHADOW_SIDE = "s"
  private val PARM_WHOLE_SHADOW = "shadow"
  private val PARM_ARC_HEIGHT = "ah"
  private val PARM_ARC_WIDTH = "aw"
  private val MONTH_SECONDS = 60 * 60 * 24 * 30
  private val EXPIRES = System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L

  private lazy val ERROR_RESPONSE = 
    InMemoryResponse(
      Array(),
      Nil, Nil, 500)

  // FIXME potential memory leak!
  // holds pre-built binaries for previously generated colors
  private val imageCache = new scala.collection.mutable.HashMap[String, Array[Byte]]

  // Rounded Corner
  // ./rounded?c=FF9900&bc=white&w=60&h=60&a=tr&sw=3&o=.5
  // ./rounded?
  //   c=FF9900&
  //   bc=white&
  //   w=60&
  //   h=60&
  //   a=tr&
  //   sw=3&
  //   o=.5
  //
  // Side Shadow
  // ./rounded?s=right&sw=2
  // ./rounded?
  //   s=right&
  //   sw=2
  //
  // Whole shadow
  // ./rounded?c=white&bc=white&w=600&h=40&shadow=true&ah=10&aw=10&sw=4&o=.5
  // ./rounded?
  //   c=white&
  //   bc=white&
  //   w=600&
  //   h=40&
  //   shadow=true&
  //   ah=10&
  //   aw=10&
  //   sw=4&
  //   o=.5

  def dispatch: LiftRules.DispatchPF = {
    // Req(url_pattern_list, suffix, request_type)
    case req@Req(SERVICE_NAME :: Nil, _, GetRequest) => () => Full(service(req))
  }

  private lazy val supportsGif = {
    var supportsGif = false
    for {name <- ImageIO.getWriterFormatNames} {
      if (name.equalsIgnoreCase("gif")) {
        supportsGif = true
      }
    }
    supportsGif
  }

  private lazy val nonTransparentFormatName = if (supportsGif) "gif" else "jpeg"

  // not sure why these aren't in  net.liftweb.util.BasicTypesHelpers
  def asFloat(in: String): Box[Float] = Helpers.tryo(in.toFloat)
  def asBoolean(in: String): Box[Boolean] = Helpers.tryo(in.toBoolean)

  def service(request:Req):LiftResponse = {
    //println("supportsGif: "+supportsGif+" got "+request)
    //println("If-Modified-Since: "+request.)

    //if (_request.getHeader("If-Modified-Since") != null) {
    //  _response.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
    //  return
    //}

    val color = request.param(PARM_COLOR)
    val bgColor = request.param(PARM_BACKGROUND_COLOR)
    val width = request.param(PARM_WIDTH).flatMap(asInt)
    val height = request.param(PARM_HEIGHT).flatMap(asInt)
    val angle = request.param(PARM_ANGLE)

    val shadowWidth = request.param(PARM_SHADOW_WIDTH).flatMap(asInt)
    val shadowOpacity = request.param(PARM_SHADOW_OPACITY).flatMap(asFloat)
    val side = request.param(PARM_SHADOW_SIDE)

    val wholeShadow = request.param(PARM_WHOLE_SHADOW).flatMap(asBoolean) openOr false
    val arcWidth = request.param(PARM_ARC_WIDTH).flatMap(asFloat)
    val arcHeight = request.param(PARM_ARC_HEIGHT).flatMap(asFloat)

    val hashKey = color :: bgColor :: width :: height :: angle :: shadowWidth :: shadowOpacity :: side :: wholeShadow :: Nil toString

    val bo = new ByteArrayOutputStream
    try {
      //XXX val imageType = if (bgColor.isDefined) nonTransparentFormatName else "png"
      val imageType = "png"

      var data = imageCache.getOrElse(hashKey, null)
      if (data != null) {
        return writeImageResponse(data, imageType)
      }

      val image =
        if (wholeShadow)
          RoundedCornerGenerator.buildShadow(color, bgColor, width, height, arcWidth, arcHeight, shadowWidth, shadowOpacity)
        else if (side.isDefined)
          RoundedCornerGenerator.buildSideShadow(side, shadowWidth, shadowOpacity)
        else
          RoundedCornerGenerator.buildCorner(color, bgColor, width, height, angle, shadowWidth, shadowOpacity)

      val success = ImageIO.write(image, imageType, bo)

      data = bo.toByteArray()

      if (! success || data == null || data.length == 0) {
        System.err.println("Image generated had zero length byte array or failed to convert from parameters :\n"
           + "[color:" + color + ", bgColor:" + bgColor
           + ", width:" + width + ", height:" + height
           + ", angle:" + angle + ", shadowWidth:" + shadowWidth
           + ", shadowOpacity:" + shadowOpacity + ", side:" + side
           + ", wholeShadow: " + wholeShadow + ", arcWidth: " + arcWidth
           + ", arcHeight:" + arcHeight + "\n image: " + image)
        return ERROR_RESPONSE
      }

      imageCache.put(hashKey, data)

      return writeImageResponse(data, imageType)
    } catch {
      case eof:IOException => 
        // ignored / expected exceptions happen when browser prematurely abandons connections - IE does this a lot
      case ex:Throwable =>
        System.err.println("Error creating image. "+ex);
        ex.printStackTrace()
    } finally {
      bo.close
    }
    ERROR_RESPONSE
  }

  def writeImageResponse(data:Array[Byte], imageType:String):LiftResponse = {
    //System.err.println("EXPIRES: "+EXPIRES+" "+Helpers.toInternetDate(EXPIRES))
    val headers = 
      ("Expires" , Helpers.toInternetDate(EXPIRES)) :: 
      ("Content-type" , "image/" + imageType) :: 
      ("Content-length" , data.length.toString) :: 
      ("X-Content-Type-Options" ,	"nosniff") ::
      ("Cache-Control" , "public, max-age=" + (MONTH_SECONDS * 3)) ::
      Nil
    InMemoryResponse(
      data,
      headers, Nil, 200)
  }
}
