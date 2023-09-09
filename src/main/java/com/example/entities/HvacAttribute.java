package com.example.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HvacAttribute {
  private String name;
  private String tag;
  private String comparator;
  private String value;
  private String unit;
  private String group;
}
