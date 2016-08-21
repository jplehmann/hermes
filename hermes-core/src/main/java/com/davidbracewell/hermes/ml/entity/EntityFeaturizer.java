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

import com.davidbracewell.apollo.ml.Feature;
import com.davidbracewell.apollo.ml.sequence.ContextualIterator;
import com.davidbracewell.apollo.ml.sequence.Sequence;
import com.davidbracewell.apollo.ml.sequence.SequenceFeaturizer;
import com.davidbracewell.hermes.Annotation;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David B. Bracewell
 */
public class EntityFeaturizer implements SequenceFeaturizer<Annotation> {
  private static final long serialVersionUID = 1L;


  @Override
  public Set<Feature> apply(ContextualIterator<Annotation> itr) {
    Set<Feature> features = new HashSet<>();


    String p0W = itr.getCurrent().toString();
    features.add(Feature.TRUE("W[0]", p0W));


    if (itr.getIndex() >= 1) {
      String p1W = itr.getPrevious(1).map(Annotation::toString).orElse(Sequence.BOS);
      features.add(Feature.TRUE("W[-1]", p1W));
      features.add(Feature.TRUE("W[-1,0]", p1W, p0W));

      if (itr.getIndex() >= 2) {
        String p2W = itr.getPrevious(2).map(Annotation::toString).orElse(Sequence.BOS);
        features.add(Feature.TRUE("W[-2]", p2W));
        features.add(Feature.TRUE("W[-2,-1]", p2W, p1W));
        features.add(Feature.TRUE("W[-2,-1,0]", p2W, p1W, p0W));
      }
    } else {
      features.add(Feature.TRUE("W[-1]", "__BOS__"));
    }


    if (itr.getIndex() + 1 < itr.size()) {
      String n1W = itr.getNext(1).map(Annotation::toString).orElse(Sequence.EOS);
      features.add(Feature.TRUE("W[0,1]", p0W, n1W));
      features.add(Feature.TRUE("W[1]", n1W));

      if (itr.getIndex() + 2 < itr.size()) {
        String n2W = itr.getNext(2).map(Annotation::toString).orElse(Sequence.EOS);
        features.add(Feature.TRUE("W[2]", n2W));
        features.add(Feature.TRUE("W[1,2]", n1W, n2W));
        features.add(Feature.TRUE("W[0,1,2]", p0W, n1W, n2W));
      }
    } else {
      features.add(Feature.TRUE("W[1]", "__EOS__"));
    }

    return features;
  }


}//END OF EntityFeaturizer
