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

package com.davidbracewell.hermes.extraction.caduceus;

import com.davidbracewell.conversion.Val;
import com.davidbracewell.hermes.RelationType;
import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author David B. Bracewell
 */
@Value
@Builder
public class CaduceusRelationProvider implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String name;
  private final String requires;
  private final RelationType relationType;
  private final String relationValue;
  private final CaduceusRelationPoint source;
  private final CaduceusRelationPoint target;
  private final boolean isReciprocal;


  static CaduceusRelationProvider fromMap(Map<String, Object> groupMap) throws IOException {
    if (!groupMap.containsKey("type")) {
      throw new IOException("No type given for: " + groupMap);
    }
    if (!groupMap.containsKey("value")) {
      throw new IOException("No value given for: " + groupMap);
    }
    if (!groupMap.containsKey("name")) {
      throw new IOException("No name given for: " + groupMap);
    }
    Map<String, Object> sourceMap = CaduceusProgram.ensureMap(groupMap.get("source"), "Source should be a map");
    Map<String, Object> targetMap = CaduceusProgram.ensureMap(groupMap.get("target"), "Target should be a map");
    return CaduceusRelationProvider.builder()
      .name(groupMap.get("name").toString())
      .requires(groupMap.containsKey("requires") ? groupMap.get("requires").toString() : null)
      .relationType(RelationType.create(groupMap.get("type").toString()))
      .relationValue(groupMap.get("value").toString())
      .isReciprocal(Val.of(groupMap.get("reciprocal")).asBooleanValue(false))
      .source(CaduceusRelationPoint.fromMap(sourceMap))
      .target(CaduceusRelationPoint.fromMap(targetMap))
      .build();
  }

}//END OF CaduceusRelationProvider
