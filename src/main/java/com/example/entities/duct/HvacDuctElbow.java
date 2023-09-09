package com.example.entities.duct;

import java.util.List;
import java.util.Objects;

public class HvacDuctElbow extends HvacDuctAttribute {
  public Double rAproxRatio = 1.5D;
  public void parseKT(List<Double> values) {
    this.setW(new AttributeValue(values.get(0), "mm"));
    this.setH(new AttributeValue(values.get(1), "mm"));
  };

  public AttributeValue getRAprox() {
    if(Objects.isNull(this.getR())) {
      return new AttributeValue(this.getW().getValue() * this.rAproxRatio, "mm");
    }
    return this.getR();
  }
}
