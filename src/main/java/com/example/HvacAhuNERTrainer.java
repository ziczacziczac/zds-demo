package com.example;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.example.entities.HvacAttribute;
import com.example.entities.TextLabel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import opennlp.tools.namefind.BioCodec;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

public class HvacAhuNERTrainer {
  private static final List<String> HVAC_OTHER_ENTITY = new ArrayList<>() {{
    add("unit");
    add("comparator");
    add("value");
    add("standard");
  }};
  public static void main(String[] args) throws IOException {
//    String trainData = createTrainingData("/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/all.jsonl");
    String trainData = "/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/data_3.txt";
    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/ner-vrv.bin"));

    TokenNameFinderModel nameFinderModel = train(trainData);
    nameFinderModel.serialize(outputStream);
    try {
      String technicalRequirements = "Loại: VRV/VRF\n"
          + "Công suất lạnh: ≥ 112 kW\n"
          + "Công suất sưởi: ≥ 125 kW\n"
          + "Số lượng Modul: ≥ 2\n"
          + "Điện năng tiêu thụ lạnh: ≤ 35.5 kW\n"
          + "Điện năng tiêu thụ sưởi: ≤ 35.51 kW\n"
          + "Thông số điện: 3P/380-415V/50Hz\n"
          + "Loại máy nén: Xoắn ốc\n"
          + "Độ ồn: ≤ 68 dB(A)";
      InputStream inputStreamTokenizer = new FileInputStream("/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/en-token.bin");
      TokenizerModel tokenModel = new TokenizerModel(inputStreamTokenizer);
      TokenizerME tokenizer = new TokenizerME(tokenModel);
      String[] sentences = technicalRequirements.split("\n");
      List<HvacAttribute> attributes = new ArrayList<>();
      for(String sentence: sentences) {
        attributes.add(extractAttribute(nameFinderModel, tokenizer, sentence, 0.7));
      }
      attributes.stream().filter(hvacAttribute -> Objects.nonNull(hvacAttribute.getName())).forEach(hvacAttribute -> {
        try {
          System.out.println(new ObjectMapper().writeValueAsString(hvacAttribute));
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (FileNotFoundException e2) {
      e2.printStackTrace();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String group = null;

  public static HvacAttribute extractAttribute(TokenNameFinderModel nameFinderModel, TokenizerME tokenizer, String sentence, double thresholdCutoff) {
    HvacAttribute hvacAttribute = new HvacAttribute();
    sentence = preprocess(sentence);
    String[] tokens = tokenizer.tokenize(sentence);

    NameFinderME nameFinder = new NameFinderME(nameFinderModel);
    Span[] nameSpans = nameFinder.find(tokens);
    for (Span name : nameSpans) {
      StringBuilder entity = new StringBuilder();
      System.out.println(name);
      for (int i = name.getStart(); i < name.getEnd(); i++) {
        entity.append(tokens[i]).append(" ");
      }
      if(name.getProb() >= thresholdCutoff) {
        if (!HVAC_OTHER_ENTITY.contains(name.getType())) {
          if (name.getType().contains("group")) {
            group = entity.toString().strip();
          } else {
            if(name.getType().contains("g|")) {
              hvacAttribute.setGroup(group);
              hvacAttribute.setTag(name.getType().split("\\|")[1]);
            } else {
              hvacAttribute.setTag(name.getType());
            }
            hvacAttribute.setName(entity.toString().strip());
          }

        } else if (name.getType().equals("unit")) {
          hvacAttribute.setUnit(entity.toString().strip());
        } else if (name.getType().equals("value")) {
          hvacAttribute.setValue(entity.toString().strip());
        } else if (name.getType().equals("comparator")) {
          hvacAttribute.setComparator(entity.toString().strip());
        }
      }

    }
    return hvacAttribute;
  }

  public static String preprocess(String sentence) {
    return sentence.strip().replaceAll("\\+", "");
  }

  public static String createTrainingData(String annotatedFile) {
    try {
      TextLabel textLabel = new ObjectMapper().readValue(new File(annotatedFile), TextLabel.class);
      int currentIndex = 0;
      textLabel.getEntities().sort((e1, e2) -> e1.getStartOffset() - e2.getEndOffset());
      List<String> entitiesLabeled = new ArrayList<>();
      for(TextLabel.Entity entity: textLabel.getEntities()) {
        String before = textLabel.getText().substring(currentIndex, entity.getStartOffset());
        if(before.equals(" ")) {
          before = "";
        }
        String entityString = textLabel.getText().substring(entity.getStartOffset(), entity.getEndOffset());
        String startLabel = " <START:" + entity.getLabel() + "> ";
        String endLabel = " <END> ";
        entitiesLabeled.add(before);
        entitiesLabeled.add(startLabel);
        entitiesLabeled.add(entityString);
        entitiesLabeled.add(endLabel);
        currentIndex = entity.getEndOffset();
        System.out.println(entityString);
      }
      FileWriter fw = new FileWriter("/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/data_3.txt");
      fw.write(String.join("", entitiesLabeled).replaceAll("  ", " "));
      fw.close();
      return "/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/data_3.txt";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static TokenNameFinderModel train(String trainingData) {
    // setting the parameters for training
    TrainingParameters params = new TrainingParameters();
    params.put(TrainingParameters.ITERATIONS_PARAM, 70);
    params.put(TrainingParameters.CUTOFF_PARAM, 1);
    params.put(TrainingParameters.ALGORITHM_PARAM, "MAXENT");

    InputStreamFactory in = null;
    try {
      in = new MarkableFileInputStreamFactory(new File(trainingData));
      ObjectStream sampleStream = new NameSampleDataStream(
          new PlainTextByLineStream(in, StandardCharsets.UTF_8));
      return NameFinderME.train("en", null, sampleStream,
          params, TokenNameFinderFactory.create(null, null, Collections.emptyMap(), new BioCodec()));
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
