package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.utils.FileUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

// 给云控服务器提供接口
@Slf4j
@NoLogin
@RestController
@RequestMapping("/api/consumer/pubres")
public class PublicResController {
    @GetMapping("/download/**")
    public void downloadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        String fileName = contextPath.substring(contextPath.lastIndexOf("/") + 1);

        Path resPath = FileUtils.resPath;
        Path filePath = Paths.get(contextPath.substring(contextPath.indexOf("download/") + "download/".length()));
        Path storePath = resPath.resolve(filePath).toAbsolutePath().normalize();

        response.reset();

        File file = resPath.resolve(storePath).toAbsolutePath().toFile();
        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            response.flushBuffer();
            return;
        }

        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode(fileName));

        response.setHeader("Cache-Control", "public");
        response.setHeader("ETag", filePath.toString());

        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), 8192)) {
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] buff = new byte[8192];
            int len = 0;
            while ((len = bis.read(buff)) != -1) {
                outputStream.write(buff, 0, len);
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("下载文件异常", e);
        }
    }
}

