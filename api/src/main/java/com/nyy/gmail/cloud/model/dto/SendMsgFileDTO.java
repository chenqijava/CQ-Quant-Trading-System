package com.nyy.gmail.cloud.model.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class SendMsgFileDTO {
    private static final long serialVersionUID = -156425782542655L;
    private String filepath;//文件路径
    private String filename;//文件名
    private int sort;//文件排序
    private long size;//文件大小
}
