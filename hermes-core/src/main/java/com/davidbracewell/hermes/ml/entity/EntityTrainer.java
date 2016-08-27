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

package com.davidbracewell.hermes.ml.entity;

import com.davidbracewell.apollo.ml.sequence.SequenceFeaturizer;
import com.davidbracewell.apollo.ml.sequence.SequenceLabelerLearner;
import com.davidbracewell.apollo.ml.sequence.TransitionFeatures;
import com.davidbracewell.apollo.ml.sequence.feature.NGramSequenceFeaturizer;
import com.davidbracewell.apollo.ml.sequence.feature.WindowedSequenceFeaturizer;
import com.davidbracewell.apollo.ml.sequence.linear.CRFTrainer;
import com.davidbracewell.apollo.ml.sequence.linear.LibraryLoader;
import com.davidbracewell.hermes.Annotation;
import com.davidbracewell.hermes.Types;
import com.davidbracewell.hermes.ml.BIOTrainer;
import com.davidbracewell.hermes.ml.BIOValidator;
import com.davidbracewell.hermes.ml.feature.PartOfSpeechFeaturizer;
import com.davidbracewell.hermes.ml.feature.WordClassFeaturizer;
import com.davidbracewell.hermes.ml.feature.WordFeaturizer;

import java.util.Collections;
import java.util.Set;

/**
 * @author David B. Bracewell
 */
public class EntityTrainer extends BIOTrainer {
   private static final long serialVersionUID = 1L;

   public EntityTrainer() {
      super("EntityTrainer", Types.ENTITY);
   }

   @Override
   @SuppressWarnings("unchecked")
   protected SequenceFeaturizer<Annotation> getFeaturizer() {
      return SequenceFeaturizer.chain(new WindowedSequenceFeaturizer<>(2, 2, new WordFeaturizer()),
                                      new WindowedSequenceFeaturizer<>(2, 2, new WordClassFeaturizer()),
                                      new WindowedSequenceFeaturizer<>(2, 2, new PartOfSpeechFeaturizer()),
                                      new NGramSequenceFeaturizer<>(3, 3, new WordFeaturizer()),
                                      new NGramSequenceFeaturizer<>(2, 2, new WordClassFeaturizer()),
                                      new NGramSequenceFeaturizer<>(2, 2, new PartOfSpeechFeaturizer()));
   }

   @Override
   public void setup() throws Exception {
      LibraryLoader.INSTANCE.load();
   }

   @Override
   protected Set<String> validTags() {
      return Collections.singleton("PERSON");
   }

   @Override
   protected SequenceLabelerLearner getLearner() {
      SequenceLabelerLearner learner = new CRFTrainer();
      learner.setTransitionFeatures(TransitionFeatures.FIRST_ORDER);
      learner.setValidator(new BIOValidator());
      learner.setParameter("maxIterations", 200);
      learner.setParameter("verbose", true);
      return learner;
   }

   public static void main(String[] args) throws Exception {
      new EntityTrainer().run(args);
   }

}//END OF EntityTrainer
