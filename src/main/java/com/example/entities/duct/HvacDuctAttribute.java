package com.example.entities.duct;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HvacDuctAttribute {
  private String type;
  private String name;
  private AttributeValue thickness;
  private AttributeValue W;
  private AttributeValue H;
  private AttributeValue L;
  private AttributeValue R;
  private AttributeValue E;
  private AttributeValue W2;
  private AttributeValue H2;
  private String material;
  private String maKem;

  public void parseKT(List<Double> values) {

  }

  public AttributeValue getArea() {
    return new AttributeValue();
  }

}
