package com.nyy.gmail.cloud.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TranslateDTO implements Serializable {

    /**
     * 消息id
     */
    private String id;

    /**
     * 源语言-默认自动检测
     */
    private String sl = "auto";

    /**
     * 目标语言
     */
    private String tl = "en";

    /**
     * 待翻译的内容
     */
    private String content;

    private Boolean historyMsg = false;


}
