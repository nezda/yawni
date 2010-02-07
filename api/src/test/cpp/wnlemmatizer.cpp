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

#include <iostream>
#include <fstream>
#include<sstream>
#include<string>
#include <wn.h>
#include <ext/hash_set>

using namespace std;
using namespace __gnu_cxx;

// Reads input from stdin (or args) and looks up lemma(s) (aka base forms) of
// the term in every POS.  Includes optional "true case" to get the lemmas in
// their actual case (e.g. "U.S.A.", not "u.s.a.").
//
// Created to compare development effort and speed to yawni.
//
// Comparing Performance
// - cat the UNIX words list through this program
//   time cat /usr/share/dict/words | ./wnlemmatizer > /dev/null
// - redirect to /dev/null to eliminate output subsystem variance
// - this is near worst case performance since no words are 
//   duplicated.
//   - however, this list is near sorted
//     - check sort: sort -c /usr/share/dict/words
//   - random shuffle would be harder ?

// Debugging Crashes
// g++ -g # compile with debugging symbols
// ulimit -a # show info including core dump limits
// ulimit -c unlimited # set no limit on size of core which can be created
// gdb ./wnlemmatizer /cores/core.12616 # run gdb & think

// bullshit requirement for non-std hash_set
namespace __gnu_cxx {
  template<> struct hash< std::string > {
    size_t operator()( const std::string& x ) const {
      return hash< const char* >()( x.c_str() );
    }
  };
}

// copy args into out
static void load(int argc, char** argv, ostream &out) {
  for(int i=1; i<argc; ++i) {
    out << " " << argv[i];
  }
}

// print readable version of pos into out
static void showPOS(int pos, ostream &out) {
  switch(pos) {
    case NOUN: out << "NOUN "; break;
    case VERB: out << "VERB "; break;
    case ADJ: out << "ADJ "; break;
    case ADJSAT: out << "ADJ "; break;
    case ADV: out << "ADV "; break;
  }
}

static const bool TRUE_CASE = false;

// print pos and any detected lemma into out
static void trueCase(char *lemma, int pos, bool &posShown, ostream &out) {
  hash_set<string> uniqueLemmas;
  for (IndexPtr iptr = 
      // exhaustive
      //getindex(lemma, pos);
      //iptr != NULL;
      //iptr = getindex(NULL, pos)
      
      // exact match
      index_lookup(lemma, pos);
      iptr != NULL;
      iptr = NULL
      ) {
    if (false == posShown) {
      showPOS(pos, out);
      posShown = true;
    }
    if (TRUE_CASE) {
      //out << " cnt: " << iptr->off_cnt << " ";
      for (unsigned long *offsets = iptr->offset, j=0;
        j < iptr->off_cnt; ++j) {
        unsigned long offset = offsets[j];
        SynsetPtr syn = read_synset(pos, offset, NULL);
        //out << " syn: " << syn->nextss << " ";
        for (int w = 0; w < syn->wcount; ++w) {
          string wordSenseLemma = syn->words[w];
          if (wordSenseLemma.empty() == false && 
            ')' == *wordSenseLemma.rbegin() &&
            string::npos != wordSenseLemma.rfind('(')) {
            const size_t lparenIdx = wordSenseLemma.rfind('(');
            wordSenseLemma = wordSenseLemma.substr(0, lparenIdx);
          }
          //out << " wordSenseLemma: " << wordSenseLemma << " ";
          //if (0 == strcasecmp(wordSenseLemma.c_str(), lemma) &&
          //  0 == uniqueLemmas.count(wordSenseLemma)) {
          if (0 == strcasecmp(wordSenseLemma.c_str(), iptr->wd) &&
            0 == uniqueLemmas.count(wordSenseLemma)) {
            //out << "words[" << w << "]: " << wordSenseLemma << " " << " whichword: " << syn->whichword << " ";
            out << wordSenseLemma << " ";
            uniqueLemmas.insert(wordSenseLemma);
            break;
          }
        }
        free_synset(syn);
      }
      //out << "*" << iptr->wd << " ";
    } else {
      out << iptr->wd << " ";
    }
    free_index(iptr);
  }
}

// look for lemmas of word in WordNet and dump results to 
// out including their associate POS 
//
// For example:
// input: "acting"
// ouput: acting NOUN acting VERB act ADJ acting
static void lemmatize(const string &word, ostream &out) {
  char* w = const_cast<char*>(word.c_str());
  for(int pos = NOUN; pos <= ADJ; ++pos) {
    bool posShown = false;
    trueCase(w, pos, posShown, out);
    for (char *lemma = morphstr(w, pos);
        lemma != NULL;
        lemma = morphstr(NULL, pos)) {
      if (false == posShown) {
        showPOS(pos, out);
        posShown = true;
      }
      // theoretically, we want to return the true case lemma,
      // e.g. not "wa" but "WA"
      // this also holds for the case where the given word
      // equalsIgnoreCase its true-case lemma
      trueCase(lemma, pos, posShown, out);
    }
  }
}

int main (int argc, char** argv) {
  wninit();

  istream *in;
  if (argc == 1) {
    // read from stdin
    in = &cin;
  } else {
    stringstream *inout = new stringstream();
    // read and lemmatize each argument
    load(argc, argv, *inout);
    in = inout;
  }
  string word;
  while (*in >> word) {
    cout << word << " ";
    lemmatize(word, cout);
    cout << endl;
  }
}
