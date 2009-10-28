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
package org.yawni.wn;

/**
 * Contents of (WordNet 3.0) lexnames file as String arrays since it is
 * "optional".  These are the WordNet lexicographers classes, sometimes called
 * the WordNet 'supersenses'.
 * <cite>M. Ciaramita, Y. Altun. 2006. Broad-Coverage Sense Disambiguation and Information Extraction with a Supersense Sequence Tagger</cite>
 * Supersense Tagging of Unknown Nouns in WordNet
 * TODO integrate descriptions into API
 * FIXME would this make a better enum ?
 */
class Lexnames {
  static final String contents [][] = {
    { "00", "adj.all", "3" },
    { "01", "adj.pert", "3 " },
    { "02", "adv.all", "4" },
    { "03", "noun.Tops", "1  " }, // abstract terms for unique beginners
    { "04", "noun.act", "1" }, // acts or actions
    { "05", "noun.animal", "1" }, // animals
    { "06", "noun.artifact", "1" }, // man-made objects
    { "07", "noun.attribute", "1" }, // attributes of people and objects
    { "08", "noun.body", "1" }, // body parts
    { "09", "noun.cognition", "1" }, // cognitive processes and contents
    { "10", "noun.communication", "1" }, // communicative processes and contents
    { "11", "noun.event", "1" }, // natural events
    { "12", "noun.feeling", "1" }, // feelings and emotions
    { "13", "noun.food", "1" }, // foods and drinks
    { "14", "noun.group", "1" }, // groupings of people or objects
    { "15", "noun.location", "1" }, // spatial position
    { "16", "noun.motive", "1" }, // goals
    { "17", "noun.object", "1" }, // natural objects (not man-made)
    { "18", "noun.person", "1" }, // people
    { "19", "noun.phenomenon", "1" }, // natural phenomena
    { "20", "noun.plant", "1" }, // plants
    { "21", "noun.possession", "1" }, // possession and transfer of possession
    { "22", "noun.process", "1" }, // natural processes
    { "23", "noun.quantity", "1" }, // quantities and units of measure
    { "24", "noun.relation", "1" }, // relations between people or things or ideas
    { "25", "noun.shape", "1" }, // two and three dimensional shapes
    { "26", "noun.state", "1" }, // stable states of affairs
    { "27", "noun.substance", "1" }, // substances
    { "28", "noun.time", "1" }, // time and temporal relations
    { "29", "verb.body", "2" }, // grooming, dressing and bodily care
    { "30", "verb.change", "2" }, // size, temperature change, intensifying
    { "31", "verb.cognition", "2" }, // thinking, judging, analyzing, doubting
    { "32", "verb.communication", "2" }, // telling, asking, ordering, singing
    { "33", "verb.competition", "2" }, // ﬁghting, athletic activities
    { "34", "verb.consumption", "2" }, // eating and drinking
    { "35", "verb.contact", "2" }, // touching, hitting, tying, digging
    { "36", "verb.creation", "2" }, // sewing, baking, painting, performing
    { "37", "verb.emotion", "2" }, // feeling
    { "38", "verb.motion", "2" }, // walking, ﬂying, swimming
    { "39", "verb.perception", "2" }, // seeing, hearing, feeling
    { "40", "verb.possession", "2" }, // buying, selling, owning
    { "41", "verb.social", "2" }, // political and social activities and events
    { "42", "verb.stative", "2" }, // being, having, spatial relations
    { "43", "verb.weather", "2" }, // raining, snowing, thawing, thundering
    { "44", "adj.ppl", "3" },
  };
}