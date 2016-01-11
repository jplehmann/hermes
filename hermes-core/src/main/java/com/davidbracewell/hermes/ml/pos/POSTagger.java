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

package com.davidbracewell.hermes.ml.pos;

import com.davidbracewell.apollo.ml.sequence.Labeling;
import com.davidbracewell.apollo.ml.sequence.SequenceFeaturizer;
import com.davidbracewell.apollo.ml.sequence.SequenceInput;
import com.davidbracewell.apollo.ml.sequence.SequenceLabeler;
import com.davidbracewell.hermes.Annotation;
import com.davidbracewell.hermes.Attrs;
import com.davidbracewell.hermes.ml.AnnotationTagger;
import com.davidbracewell.hermes.tag.POS;

/**
 * @author David B. Bracewell
 */
public class POSTagger extends AnnotationTagger {
  /**
   * The Featurizer.
   */
  final SequenceFeaturizer<Annotation> featurizer;
  /**
   * The Labeler.
   */
  final SequenceLabeler labeler;

  public POSTagger(SequenceFeaturizer<Annotation> featurizer, SequenceLabeler labeler) {
    this.featurizer = featurizer;
    this.labeler = labeler;
  }

  @Override
  public void tag(Annotation sentence) {
    SequenceInput<Annotation> sequenceInput = new SequenceInput<>(sentence.tokens());
    Labeling result = labeler.label(featurizer.extractSequence(sequenceInput.iterator()));
    for (int i = 0; i < sentence.tokenLength(); i++) {
      sentence.tokenAt(i).put(Attrs.PART_OF_SPEECH, POS.fromString(result.getLabel(i)));
    }
  }

}// END OF POSTagger
