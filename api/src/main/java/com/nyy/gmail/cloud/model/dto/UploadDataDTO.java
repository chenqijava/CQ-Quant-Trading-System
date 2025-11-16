package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

@Data
@ToString
@Accessors(chain = true)
public class UploadDataDTO implements Serializable {


    private static final long serialVersionUID = 1773595072381763094L;


    private String addMethod;

    private String addData;

    private String filePath;

    private String fileName;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UploadDataDTO other)) {
            return false;
        }

        // 添加方法存在且一样
        if (StringUtils.isAnyBlank(addMethod, other.addMethod) || !addMethod.equals(other.addMethod)) {
            return false;
        }

        if ("1".equals(addMethod)) {
            return !StringUtils.isAnyBlank(addData, other.addData) && addData.equals(other.addData);
        } else if ("2".equals(addMethod)) {
            return !StringUtils.isAnyBlank(filePath, other.filePath) && filePath.equals(other.filePath);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addMethod, addData, filePath);
    }

}
