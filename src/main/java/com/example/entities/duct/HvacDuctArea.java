package com.example.entities.duct;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HvacDuctArea {
  private HvacDuctAttribute duct;
  private Double lossRatio;

  public AttributeValue getTotalArea() {
    AttributeValue area = duct.getArea();
    return new AttributeValue(area.getValue() * lossRatio, area.getUnit());
  }
}
