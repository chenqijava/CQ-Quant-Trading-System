package com.nyy.gmail.cloud.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class TranslateResult implements Serializable {

    /**
     * 消息id
     */
    private String id;

    /**
     * 待翻译的内容
     */
    private String content;

    /**
     * 目标语言
     */
    private String tl;

}
