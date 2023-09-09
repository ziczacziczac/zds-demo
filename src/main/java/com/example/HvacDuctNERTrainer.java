package com.example;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.w3c.dom.Attr;
import com.example.entities.duct.AttributeValue;
import com.example.entities.duct.HvacDuctArea;
import com.example.entities.duct.HvacDuctAttribute;
import com.example.entities.TextLabel;
import com.example.entities.duct.HvacDuctElbow;
import com.example.entities.duct.HvacDuctQuyDoi;
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

public class HvacDuctNERTrainer {
  private static final List<String> DUCT_NAMES = new ArrayList<>() {{
    add("ELBOW");
    add("DUCT");
  }};

  private static final Map<String, HvacDuctQuyDoi> DUCT_QUY_DOI_MAP = new HashMap<>() {{
    put("Cút 90", new HvacDuctQuyDoi("Cút 90", new AttributeValue(0.75, "mm"), 0.2, "W*H"));
  }};

  public static void main(String[] args) throws IOException {
    String trainData = "/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/data_duct.txt";
    TokenNameFinderModel nameFinderModel = train(trainData);
    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream("/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/ner-duct.bin"));
    nameFinderModel.serialize(outputStream);
    try {
      String technicalRequirements = "Cút 90 KT 750x400 bằng tôn tráng kẽm dày 0.75 mm, mạ kẽm Z12";
      InputStream inputStreamTokenizer = new FileInputStream("/Users/doquangdat/Documents/python/quarkus/demo/src/main/resources/en-token.bin");
      TokenizerModel tokenModel = new TokenizerModel(inputStreamTokenizer);
      TokenizerME tokenizer = new TokenizerME(tokenModel);
      String[] sentences = technicalRequirements.split("\n");
      List<HvacDuctAttribute> attributes = new ArrayList<>();
      for(String sentence: sentences) {
        attributes.add(extractAttribute(nameFinderModel, tokenizer, sentence, 0.7));
      }
      attributes.stream().filter(hvacAttribute -> Objects.nonNull(hvacAttribute.getName())).forEach(hvacAttribute -> {
        try {
          System.out.println(new ObjectMapper().writeValueAsString(hvacAttribute));
          HvacDuctQuyDoi quyDoi = DUCT_QUY_DOI_MAP.get(hvacAttribute.getName());
          System.out.println(quyDoi.getCongThucTinhDienTich());
          Arrays.stream(hvacAttribute.getClass().getSuperclass().getDeclaredFields()).forEach(field -> {
            field.setAccessible(true);
            try {
              Object value = field.get(hvacAttribute);
              if(value instanceof AttributeValue) {
                System.out.println(value);
                quyDoi.setCongThucTinhDienTich(quyDoi.getCongThucTinhDienTich().replaceAll(field.getName(), String.valueOf(((AttributeValue)value).getValue().intValue())));
              }

            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }

          });

          System.out.println(quyDoi.getCongThucTinhDienTich());
          System.out.println(ExpressionEvaluation.evaluate(quyDoi.getCongThucTinhDienTich()));
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

  public static HvacDuctAttribute extractAttribute(TokenNameFinderModel nameFinderModel, TokenizerME tokenizer, String sentence, double thresholdCutoff) {
    HvacDuctAttribute hvacDuct = null;
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
      String entityValue = entity.toString().strip();
      if(name.getProb() >= thresholdCutoff) {
        if (DUCT_NAMES.contains(name.getType())) {
          if(entityValue.equals("Cút 90")) {
            hvacDuct = new HvacDuctElbow();
          } else {
            hvacDuct = new HvacDuctAttribute();
          }
          hvacDuct.setName(entityValue);
        }
        else if (name.getType().equals("kich_thuoc")) {
          //TODO: matching kich thuoc
          List<Double> values = parseNumber(entityValue);
          hvacDuct.parseKT(values);
        } else if (name.getType().equals("material")) {
          hvacDuct.setMaterial(entityValue);
        } else if (name.getType().equals("thickness")) {
          List<Double> values = parseNumber(entityValue);
          if(values.size() > 0) {
            hvacDuct.setThickness(new AttributeValue(values.get(0), "mm"));
          }
        } else if (name.getType().equals("ma_kem")) {
          hvacDuct.setMaKem(entityValue);
        }
      }

    }
    return hvacDuct;
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

  public static List<Double> parseNumber(String value) {
    List<Double> values = new ArrayList<>();
    Pattern p = Pattern.compile("-?\\d+\\.?\\d+");
    Matcher m = p.matcher(value);
    while(m.find()) {
      values.add(Double.valueOf(m.group()));
    }
    return values;
  }
}
