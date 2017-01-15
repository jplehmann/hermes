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

package com.davidbracewell.hermes.processor;

import com.davidbracewell.config.Config;
import com.davidbracewell.conversion.Cast;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.Serializable;
import java.util.Map;

/**
 * @author David B. Bracewell
 */

public class ProcessorContext implements Serializable {
   private static final long serialVersionUID = 1L;
   private final Map<String, Object> properties = new UnifiedMap<>();


   public ProcessorContext() {

   }

   @Builder
   public ProcessorContext(@Singular @NonNull Map<String, ?> properties) {
      this.properties.putAll(properties);
   }

   public <T> T getAs(String name, @NonNull Class<T> clazz) {
      if (properties.containsKey(name)) {
         return Cast.as(properties.get(name), clazz);
      }
      return Config.get(name).as(clazz);

   }

   public <T> T getAs(String name, @NonNull Class<T> clazz, T defaultValue) {
      if (properties.containsKey(name)) {
         return Cast.as(properties.getOrDefault(name, defaultValue), clazz);
      }
      return Config.get(name).as(clazz, defaultValue);
   }

   public String getString(String name) {
      return getAs(name, String.class);
   }

   public Double getDouble(String name) {
      return getAs(name, Double.class);
   }

   public Integer getInteger(String name) {
      return getAs(name, Integer.class);
   }

   public String getString(String name, String defaultValue) {
      return getAs(name, String.class, defaultValue);
   }

   public Double getDouble(String name, double defaultValue) {
      return getAs(name, Double.class, defaultValue);
   }

   public Integer getInteger(String name, int defaultValue) {
      return getAs(name, Integer.class, defaultValue);
   }

   public void property(String name, Object value) {
      this.properties.put(name, value);
   }

}//END OF ProcessorContext