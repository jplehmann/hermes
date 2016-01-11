package com.davidbracewell.hermes.corpus.spi;

import com.davidbracewell.hermes.Annotation;
import com.davidbracewell.hermes.Document;

import java.util.List;

/**
 * @author David B. Bracewell
 */
public enum WordProcessor implements FieldProcessor {
  INSTANCE;

  @Override
  public void process(Document document, List<List<String>> rows) {

  }

  @Override
  public String processOutput(Annotation sentence, Annotation token, int index) {
    return token.toString();
  }

}// END OF WordProcessor