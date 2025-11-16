package com.nyy.gmail.cloud.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class StatisticsAccountDTO implements Serializable {

    private Long total;
    private Long onlineTotal;
    private Long unlineTotal;
    private Long bannedTotal;

}
