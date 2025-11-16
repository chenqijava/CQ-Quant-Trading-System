package com.nyy.gmail.cloud.model.vo;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class FailedVO {
    List<String> failedType = new ArrayList<>();
    Map<String, List<String>> failed = new HashMap<>();

    public void addFailed(String message) {
        addFailed(message, null);
    }

    public void addFailed(String message, String desc) {
        if (failedType.indexOf(message) == -1) {
            failed.put(message, new ArrayList<>());
        }
        failedType.add(message);
        if (desc != null) failed.get(message).add(desc);
    }
}
