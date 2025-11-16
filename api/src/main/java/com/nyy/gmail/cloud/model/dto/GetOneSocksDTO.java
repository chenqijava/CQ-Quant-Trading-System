package com.nyy.gmail.cloud.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@ToString
@Accessors(chain = true)
public class GetOneSocksDTO implements Serializable {
    private static final long serialVersionUID = -4032347391909747488L;
    @NotEmpty
    private String account_id; // account._id
    private String phoneNumber;
}
