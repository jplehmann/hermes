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

package com.davidbracewell.hermes;

import com.davidbracewell.Language;
import com.davidbracewell.cache.Cache;
import com.davidbracewell.cache.CacheManager;

import javax.annotation.Nonnull;

/**
 * <p>Factory with cache for constructing/retrieving annotators for a given annotation class. The cache is managed
 * using {@link CacheManager} and is named <code>com.davidbracewell.text.annotatorCache</code>.
 *
 * @author David B. Bracewell
 */
public class AnnotatorCache {

  private static volatile AnnotatorCache INSTANCE;
  private final Cache<String, Annotator> cache;

  private AnnotatorCache() {
    cache = CacheManager.getInstance().get("hermes.AnnotatorCache");
  }

  /**
   * @return The instance of the <code>AnnotatorFactory</code>
   */
  public static AnnotatorCache getInstance() {
    if (INSTANCE == null) {
      synchronized (AnnotatorCache.class) {
        if (INSTANCE == null) {
          INSTANCE = new AnnotatorCache();
        }
      }
    }
    return INSTANCE;
  }

  /**
   * Gets (from cache or constructs) the annotator specified via the configs for a given annotation type. Annotators
   * are specified by using the fully qualified name of the annotation and .annotator. As with all config values the
   * setting can be language specific. Configs will be checked for the language of the passed in. An example of a
   * settings is: <code>AnnotationType.TOKEN.annotator=com.davidbracewell.annotation.Tokenizer</code>
   *
   * @param annotationType The annotation type
   * @param language       The language of the annotator we want
   * @return An annotator that can annotate the given annotation class
   */
  public Annotator get(@Nonnull AnnotationType annotationType, @Nonnull Language language) {
    String key = createKey(annotationType, language);
    if (!cache.containsKey(key)) {
      cache.putIfAbsent(key, annotationType.getAnnotator(language));
    }
    return cache.get(key);
  }

  private String createKey(AnnotationType type, Language language) {
    return type.name() + "::" + language;
  }

  /**
   * Invalidates an item in the cache of
   *
   * @param annotationType the annotation type
   * @param language       The language
   */
  public void invalidate(@Nonnull AnnotationType annotationType, @Nonnull Language language) {
    cache.invalidate(createKey(annotationType, language));
  }

  /**
   * Invalidates the cache
   */
  public void clear() {
    cache.clear();
  }


}//END OF AnnotatorFactory