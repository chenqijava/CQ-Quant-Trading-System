package com.nyy.gmail.cloud.model.vo;

import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DnsRecordVO {

    private String workName;
    private Long count;
    private Date latestCreateTime;
    private String domain;
    private List<String> dnsList;
    private BigDecimal rate;
}
