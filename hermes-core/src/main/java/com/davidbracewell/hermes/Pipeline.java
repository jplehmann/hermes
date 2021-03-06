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
import com.davidbracewell.apollo.ml.data.DatasetType;
import com.davidbracewell.concurrent.Broker;
import com.davidbracewell.concurrent.IterableProducer;
import com.davidbracewell.config.Config;
import com.davidbracewell.guava.common.base.Preconditions;
import com.davidbracewell.guava.common.base.Stopwatch;
import com.davidbracewell.guava.common.base.Throwables;
import com.davidbracewell.hermes.annotator.Annotator;
import com.davidbracewell.hermes.corpus.Corpus;
import com.davidbracewell.hermes.corpus.CorpusFormats;
import com.davidbracewell.io.MultiFileWriter;
import com.davidbracewell.io.Resources;
import com.davidbracewell.io.resource.Resource;
import com.davidbracewell.logging.Logger;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * <p>A pipeline wraps the process of annotating a document with one or more annotations. By constructing a pipeline
 * documents can be processed in parallel possibly lowering the amount of time needed to annotation a document
 * collection.</p>
 *
 * @author David B. Bracewell
 */
public final class Pipeline implements Serializable {

   private static final Logger log = Logger.getLogger(Pipeline.class);
   private static final long serialVersionUID = 1L;
   private final AnnotatableType[] annotationTypes;
   private final int numberOfThreads;
   private final Stopwatch timer = Stopwatch.createUnstarted();
   private final java.util.function.Consumer<Document> onComplete;
   private final int queueSize;
   private final boolean returnCorpus;
   private long totalTime;
   private AtomicLong documentsProcessed = new AtomicLong();


   private Pipeline(int numberOfThreads, int queueSize, Consumer<Document> onComplete, Collection<AnnotatableType> annotationTypes, boolean returnCorpus) {
      this.returnCorpus = returnCorpus;
      Preconditions.checkArgument(numberOfThreads > 0, "Number of threads must be > 0");
      Preconditions.checkArgument(queueSize > 0, "Queue size must be > 0");
      this.queueSize = queueSize;
      this.annotationTypes = Preconditions
                                .checkNotNull(annotationTypes)
                                .toArray(new AnnotatableType[annotationTypes.size()]);
      this.numberOfThreads = numberOfThreads;
      this.onComplete = Preconditions.checkNotNull(onComplete);
   }

   /**
    * Convenience method for getting a Pipeline Builder
    *
    * @return the pipeline builder
    */
   public static Builder builder() {
      return new Builder();
   }

   public static void process(@NonNull Document document, Annotator... annotators) {
      if (annotators == null || annotators.length == 0) {
         return;
      }

      for (Annotator annotator : annotators) {
         if (annotator.satisfies().stream().allMatch(document::isCompleted)) {
            continue;
         }
         for (AnnotatableType type : annotator.requires()) {
            process(document, type);
         }
         annotator.annotate(document);
         for (AnnotatableType type : annotator.satisfies()) {
            document
               .getAnnotationSet()
               .setIsCompleted(type, true, annotator.getClass().getName() + "::" + annotator.getVersion());
         }
      }
   }

   /**
    * Annotates a document with the given annotation types.
    *
    * @param textDocument    the document to be the annotate
    * @param annotationTypes the annotation types to be annotated
    */
   public static void process(@NonNull Document textDocument, AnnotatableType... annotationTypes) {
      if (annotationTypes == null || annotationTypes.length == 0) {
         return;
      }

      for (AnnotatableType annotationType : annotationTypes) {
         if (annotationType == null) {
            continue;
         }

         if (textDocument.getAnnotationSet().isCompleted(annotationType)) {
            continue;
         }

         if (log.isLoggable(Level.FINEST)) {
            log.finest("Annotating for {0}", annotationType);
         }

         Annotator annotator = AnnotatorCache.getInstance().get(annotationType, textDocument.getLanguage());

         if (annotator == null) {
            throw new IllegalStateException("Could not get annotator for " + annotationType);
         }

         if (!annotator.satisfies().contains(annotationType)) {
            throw new IllegalStateException(annotator.getClass().getName() + " does not satisfy " + annotationType);
         }

         //Get the requirements out of the way
         for (AnnotatableType prereq : annotator.requires()) {
            process(textDocument, prereq);
         }

         annotator.annotate(textDocument);
         for (AnnotatableType type : annotator.satisfies()) {
            textDocument
               .getAnnotationSet()
               .setIsCompleted(type, true, annotator.getClass().getName() + "::" + annotator.getVersion());
         }

      }
   }

   public static void setAnnotator(@NonNull AnnotationType annotationType, @NonNull Language language, @NonNull Annotator annotator) {
      AnnotatorCache.getInstance().setAnnotator(annotationType, language, annotator);
   }

   public double getElapsedTime(@NonNull TimeUnit timeUnit) {
      double totalNanoSeconds = totalTime + timer.elapsed(TimeUnit.NANOSECONDS);
      if (timeUnit == TimeUnit.NANOSECONDS) {
         return totalNanoSeconds;
      }
      return totalNanoSeconds / TimeUnit.NANOSECONDS.convert(1, timeUnit);
   }

   /**
    * The number of documents processed per second
    *
    * @return the number of documents processed per second
    */
   public double documentsPerSecond() {
      return (double) documentsProcessed.get() / getElapsedTime(TimeUnit.SECONDS);
   }

   /**
    * Annotates documents with the annotation types defined in the pipeline.
    *
    * @param documents the source of documents to be annotated
    */
   @SneakyThrows
   public Corpus process(@NonNull Corpus documents) {
      timer.start();

      Broker.Builder<Document> builder = Broker.<Document>builder()
                                            .addProducer(new IterableProducer<>(documents))
                                            .bufferSize(queueSize);

      Corpus corpus = documents;
      if (returnCorpus && corpus.getDataSetType() == DatasetType.OffHeap) {
         Resource tempFile = Resources.temporaryDirectory();
         tempFile.deleteOnExit();
         try (MultiFileWriter writer = new MultiFileWriter(tempFile, "part-", Config.get("files.partition")
                                                                                    .asIntegerValue(numberOfThreads))) {
            builder.addConsumer(new AnnotateConsumer(annotationTypes, onComplete, documentsProcessed, writer),
                                numberOfThreads)
                   .build()
                   .run();
         }
         corpus = Corpus.builder().offHeap().source(CorpusFormats.JSON_OPL, tempFile).build();
      } else {
         builder.addConsumer(new AnnotateConsumer(annotationTypes, onComplete, documentsProcessed, null),
                             numberOfThreads)
                .build()
                .run();
      }

      timer.stop();
      totalTime += timer.elapsed(TimeUnit.NANOSECONDS);
      timer.reset();

      return corpus;
   }

   public Document process(@NonNull Document document) {
      timer.start();
      process(document, annotationTypes);
      timer.stop();
      documentsProcessed.incrementAndGet();
      totalTime += timer.elapsed(TimeUnit.NANOSECONDS);
      timer.reset();
      return document;
   }

   /**
    * Total time processing.
    *
    * @return the total time processing in string representation
    */
   public String totalTimeProcessing() {
      return String.format("%.4g s", getElapsedTime(TimeUnit.SECONDS));
   }

   private enum NoOpt implements java.util.function.Consumer<Document> {
      INSTANCE;

      @Override
      public void accept(Document input) {
      }
   }

   /**
    * A builder class for pipelines
    */
   public static class Builder {

      int queueSize = 10000;
      Set<AnnotatableType> annotationTypes = new HashSet<>();
      int numberOfThreads = Runtime.getRuntime().availableProcessors();
      java.util.function.Consumer<Document> onComplete = NoOpt.INSTANCE;
      boolean returnCorpus = true;

      /**
       * Add annotation.
       *
       * @param annotation the annotation
       * @return the builder
       */
      public Builder addAnnotation(AnnotatableType annotation) {
         annotationTypes.add(Preconditions.checkNotNull(annotation));
         return this;
      }

      /**
       * Add annotations.
       *
       * @param annotations the annotations
       * @return the builder
       */
      public Builder addAnnotations(AnnotatableType... annotations) {
         Preconditions.checkNotNull(annotations);
         this.annotationTypes.addAll(Arrays.asList(annotations));
         return this;
      }

      public Builder returnCorpus(boolean returnCorpus) {
         this.returnCorpus = returnCorpus;
         return this;
      }

      /**
       * Build pipeline.
       *
       * @return the pipeline
       */
      public Pipeline build() {
         return new Pipeline(numberOfThreads, queueSize, onComplete, annotationTypes, returnCorpus);
      }

      /**
       * Number of threads.
       *
       * @param threadCount the thread count
       * @return the builder
       */
      public Builder numberOfThreads(int threadCount) {
         this.numberOfThreads = threadCount;
         return this;
      }

      /**
       * On complete.
       *
       * @param onComplete the on complete
       * @return the builder
       */
      public Builder onComplete(java.util.function.Consumer<Document> onComplete) {
         this.onComplete = onComplete;
         return this;
      }

      /**
       * Queue size.
       *
       * @param queueSize the queue size
       * @return the builder
       */
      public Builder queueSize(int queueSize) {
         this.queueSize = queueSize;
         return this;
      }

   }//END OF Pipeline$Builder

   private class AnnotateConsumer implements java.util.function.Consumer<Document>, Serializable {
      private static final long serialVersionUID = 1L;
      private final AnnotatableType[] annotationTypes;
      private final java.util.function.Consumer<Document> onComplete;
      private final AtomicLong counter;
      private final MultiFileWriter writer;

      private AnnotateConsumer(AnnotatableType[] annotationTypes, Consumer<Document> onComplete, AtomicLong counter, MultiFileWriter writer) {
         this.annotationTypes = annotationTypes;
         this.onComplete = onComplete;
         this.counter = counter;
         this.writer = writer;
      }

      @Override
      public void accept(Document document) {
         if (document != null) {
            process(document, annotationTypes);
            long count = counter.incrementAndGet();
            if (count % 5_000 == 0) {
               System.err.println(count + " (" + documentsPerSecond() + ")");
            }
            if (writer != null) {
               try {
                  writer.write(document.toJson() + "\n");
               } catch (IOException e) {
                  throw Throwables.propagate(e);
               }
            }
            onComplete.accept(document);
         }
      }

   }

}//END OF Pipeline
