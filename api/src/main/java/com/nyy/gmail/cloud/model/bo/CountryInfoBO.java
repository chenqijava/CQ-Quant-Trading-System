package com.nyy.gmail.cloud.model.bo;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class CountryInfoBO {
    private String name;
    private String areaCode;
    private String countryName;
    private String englishCountryName;

}
