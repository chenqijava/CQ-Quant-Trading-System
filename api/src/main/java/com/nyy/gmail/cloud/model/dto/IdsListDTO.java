package com.nyy.gmail.cloud.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdsListDTO {


    private List<String> ids;

    private String id;

    public IdsListDTO(List<String> ids) {
        this.ids = ids;
    }
}
