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

package com.davidbracewell.hermes.annotator;

import com.davidbracewell.collection.Collect;
import com.davidbracewell.hermes.*;
import com.davidbracewell.string.StringUtils;

import java.io.Serializable;
import java.text.BreakIterator;
import java.util.Collections;
import java.util.Set;

/**
 * @author David B. Bracewell
 */
public class DefaultTokenAnnotator implements Annotator, Serializable {
  private static final long serialVersionUID = 1L;

  @Override
  public void annotate(Document document) {
    BreakIterator iterator = BreakIterator.getWordInstance(document.getLanguage().asLocale());
    iterator.setText(document.toString());
    int index = 0;
    for (int end = iterator.next(), start = 0; end != BreakIterator.DONE; end = iterator.next()) {
      if (!StringUtils.isNullOrBlank(document.subSequence(start, end).toString())) {
        document.createAnnotation(Types.TOKEN, start, end, Collect.map(Attrs.INDEX, index));
        index++;
      }
      start = end;
    }
  }

  @Override
  public Set<AnnotationType> provides() {
    return Collections.singleton(Types.TOKEN);
  }

  @Override
  public Set<AnnotationType> requires() {
    return Collections.emptySet();
  }

}//END OF DefaultTokenAnnotator