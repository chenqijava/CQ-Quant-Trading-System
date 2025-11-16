package com.nyy.gmail.cloud.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.nyy.gmail.cloud.model.dto.GraphCodeDTO;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.io.IORuntimeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CaptchaUtils {

    public static final String CAPTCHA_IMAGE_PATH = "graphCaptcha";
    public static final Long ONE_DAY_MILLIS = 1000L * 60L * 60L * 24L;

    public static Path getCaptchaImagePath() {
        return FileUtils.resPath.resolve(CAPTCHA_IMAGE_PATH);
    }

    // 删除1天以前的图形验证码
    public static void clearCaptchaImage() {
        FileUtils.deleteFile(getCaptchaImagePath(), ONE_DAY_MILLIS);
    }

    /**
     * 生成图形验证码
     *
     * @return
     */
    public static GraphCodeDTO generateGraphCaptchaCode() {
        // 生成图形验证码下载文件夹(使用download接口的下载地址)
        Path downloadPath = Paths.get(CAPTCHA_IMAGE_PATH);
        // 生成图形验证码存储文件夹
        Path realPath = FileUtils.resPath.resolve(downloadPath);
        // 判断图形验证码存储路径是否存在
        File filePath = new File(realPath.toUri());
        // 不存在则创建
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        // 开始生成图形验证码
        // LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(100, 30, 5, 20);
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(100, 30, new RandomGenerator("0123456789", 5), 20);
        String uuid = UUID.randomUUID().toString();
        // 设置图形验证码文件名
        String file = uuid + ".png";
        // 设置图形验证码下载路径
        String downloadFile = downloadPath + File.separator + file;
        // 设置图形验证码存储路径
        String realFile = filePath + File.separator + file;
        try {
            // 写入图形验证码
            lineCaptcha.write(realFile);
        } catch (IORuntimeException ioRuntimeException) {
            log.error("生成图形验证码错误", ioRuntimeException);
            downloadFile = null;
        }
    
        if (downloadFile == null) {
            return null;
        }
    
        GraphCodeDTO graphCodeDTO = new GraphCodeDTO()
                .setGraphCode(lineCaptcha.getCode())
                .setGraphCodeKey(uuid)
                .setPath(file);
        return graphCodeDTO;
    }
    
}
