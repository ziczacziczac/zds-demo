package com.example.entities.duct;

import java.util.Stack;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HvacDuctQuyDoi {
  private String name;
  private AttributeValue thickness;
  private Double tiLeHaoHut;
  private String congThucTinhDienTich;
}
