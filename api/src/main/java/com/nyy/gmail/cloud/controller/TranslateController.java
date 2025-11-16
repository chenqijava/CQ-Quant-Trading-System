package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.model.dto.TranslateDTO;
import com.nyy.gmail.cloud.model.dto.TranslateResult;
import com.nyy.gmail.cloud.service.TranslateService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequiredPermission(MenuType.globalParams)
@RequestMapping({"/api/chatroom/"})
public class TranslateController {

    @Resource
    private TranslateService translateService;


    @PostMapping("/translate")
    public Result<String> translate(@RequestBody TranslateDTO translateDTO) {

        if (StringUtils.isBlank(translateDTO.getContent())) {
            return ResponseResult.success("");
        }
        TranslateResult translate = translateService.translate(translateDTO);
        return ResponseResult.success(translate.getContent());
    }

}
