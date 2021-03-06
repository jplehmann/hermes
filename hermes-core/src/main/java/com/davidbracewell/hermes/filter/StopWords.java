/*
 * (c) 2005 David B. Bracewell
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.davidbracewell.hermes.filter;

import com.davidbracewell.Language;
import com.davidbracewell.config.Config;
import com.davidbracewell.conversion.Cast;
import com.davidbracewell.function.SerializablePredicate;
import com.davidbracewell.guava.common.collect.Maps;
import com.davidbracewell.hermes.Annotation;
import com.davidbracewell.hermes.HString;
import com.davidbracewell.hermes.Types;

import java.io.Serializable;
import java.util.Map;

/**
 * The type Stop words.
 *
 * @author David B. Bracewell
 */
public abstract class StopWords implements Serializable {
  private static final long serialVersionUID = 1L;

  private static volatile Map<Language, StopWords> stopWordLists = Maps.newConcurrentMap();

  /**
   * Gets instance.
   *
   * @param language the language
   * @return the instance
   */
  public static StopWords getInstance(Language language) {
    if (!stopWordLists.containsKey(language)) {
      synchronized (StopWords.class) {
        if (!stopWordLists.containsKey(language)) {
          stopWordLists.put(language, Config.get("hermes.StopWords", language, "class").as(StopWords.class, new NoOptStopWords()));
        }
      }
    }
    return stopWordLists.get(language);
  }

  protected abstract boolean isTokenStopWord(Annotation token);

  /**
   * Is stop word.
   *
   * @param word the word
   * @return the boolean
   */
  public abstract boolean isStopWord(String word);

  /**
   * Is stop word.
   *
   * @param text the text
   * @return the boolean
   */
  public boolean isStopWord(HString text) {
    if (text == null) {
      return true;
    } else if (text.isInstance(Types.TOKEN)) {
      return isTokenStopWord(Cast.as(text));
    }
    return text.tokens().stream().allMatch(this::isTokenStopWord);
  }

  /**
   * Returns true if any token in the supplied text is a stop word.
   * @param text
   * @return
   */
  public boolean hasStopWord(HString text) {
    if (text == null) {
      return true;
    } else if (text.isInstance(Types.TOKEN)) {
      return isTokenStopWord(Cast.as(text));
    }
    return text.tokens().stream().anyMatch(this::isTokenStopWord);
  }

  public static SerializablePredicate<HString> hasStopWord() {
    return hString -> {
      if (hString == null) {
        return true;
      }
      return hString.tokens().stream().anyMatch(isStopWord());
    };
  }

  public static SerializablePredicate<HString> notHasStopWord() {
    return hasStopWord().negate();
  }

  public static SerializablePredicate<HString> isStopWord() {
    return hString -> {
      if (hString == null) {
        return true;
      }
      return StopWords.getInstance(hString.getLanguage()).isStopWord(hString);
    };
  }

  public static SerializablePredicate<HString> isNotStopWord() {
    return isStopWord().negate();
  }

  /**
   * The type No opt stop words.
   */
  public static class NoOptStopWords extends StopWords {
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean isTokenStopWord(Annotation token) {
      return false;
    }

    @Override
    public boolean isStopWord(String word) {
      return false;
    }


  }//END OF StopWords$EmptyStopWords


}//END OF StopWords
