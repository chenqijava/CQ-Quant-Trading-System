package com.nyy.gmail.cloud.controller;

import cn.hutool.core.io.FileUtil;
import com.nyy.gmail.cloud.utils.ImageCompressor;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.yaml.snakeyaml.util.UriEncoder;

import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.model.vo.UploadFileVO;
import com.nyy.gmail.cloud.utils.CaptchaUtils;
import com.nyy.gmail.cloud.utils.FileUtils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 用于文件的上传和下载
 */
@Slf4j
@RestController
@RequestMapping({"/api/consumer/res", "/api/chatroom/res"})
public class ResController {

    @PostMapping("/upload/**")
    public Result<UploadFileVO> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 允许的图片 MIME 类型
        List<String> allowedContentTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
        );

        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase())) {
            log.warn("上传失败，文件类型不被允许: {}", contentType);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传图片类型文件");
        }

        // 2. 校验文件扩展名（进一步确认）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches("(?i)^.+\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            log.warn("上传失败，文件扩展名不合法: {}", originalFilename);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传图片文件（.jpg/.png/.gif/.bmp/.webp）");
        }

        // 获取路径
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setName(originalFilename);
        uploadFileVO.setType(contentType);

        Path resPath = FileUtils.resPath;
        uploadFileVO.setType(contextPath.substring(contextPath.indexOf("upload") + "upload/".length()));
        Path folder = Paths.get(uploadFileVO.getType());
        try {
            Files.createDirectories(resPath.resolve(folder).normalize());
        } catch (IOException e) {
            log.error("创建upload文件夹失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        String ext = null;
        String oldFileName = null;
        try {
            ext = FileUtil.getSuffix(originalFilename);
            oldFileName = FileUtil.getPrefix(originalFilename);
        } catch (Exception e2) {
            log.error("根据文件名获取文件后缀失败", e2);
        }

//        String fileName = StringUtils.cleanPath(oldFileName + "-" + System.currentTimeMillis() + ".jpg");
        String fileName = StringUtils.cleanPath(oldFileName + "-" + System.currentTimeMillis() + (ext == null ? "" : "." + ext));
        if (fileName.contains("..")) {
            log.error("文件名不能包含.. {}", fileName);
            return ResponseResult.failure(ResultCode.FILENAME_BAD_REQUEST);
        }

        Path filePath = folder.resolve(fileName).normalize();
        Path storePath = resPath.resolve(filePath).normalize();
        uploadFileVO.setFilepath(filePath.toString());

        try {
            Files.copy(file.getInputStream(), storePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件存储失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }
//        try {
//            ImageCompressor.compressAndClean(file.getInputStream(), storePath);
//        } catch (IOException e) {
//            log.error("图片压缩写入失败", e);
//            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR, "图片处理失败");
//        }

        return ResponseResult.success(uploadFileVO);
    }


    @PostMapping("/uploadTxt/**")
    public Result<UploadFileVO> uploadFileTxt(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            log.warn("上传失败，文件类型不被允许: {}", contentType);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传图片类型文件");
        }

        // 2. 校验文件扩展名（进一步确认）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches("(?i)^.+\\.(json|txt|csv)$")) {
            log.warn("上传失败，文件扩展名不合法: {}", originalFilename);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传文件（.json/.txt/.csv）");
        }

        // 获取路径
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setName(originalFilename);
        uploadFileVO.setType(contentType);

        Path resPath = FileUtils.resPath;
        uploadFileVO.setType(contextPath.substring(contextPath.indexOf("upload") + "uploadTxt/".length()));
        Path folder = Paths.get(uploadFileVO.getType());
        try {
            Files.createDirectories(resPath.resolve(folder).normalize());
        } catch (IOException e) {
            log.error("创建upload文件夹失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        String ext = null;
        String oldFileName = null;
        try {
            ext = FileUtil.getSuffix(originalFilename);
            oldFileName = FileUtil.getPrefix(originalFilename);
        } catch (Exception e2) {
            log.error("根据文件名获取文件后缀失败", e2);
        }

        String fileName = StringUtils.cleanPath(oldFileName + "-" + System.currentTimeMillis() + "." + ext);
        if (fileName.contains("..")) {
            log.error("文件名不能包含.. {}", fileName);
            return ResponseResult.failure(ResultCode.FILENAME_BAD_REQUEST);
        }

        Path filePath = folder.resolve(fileName).normalize();
        Path storePath = resPath.resolve(filePath).normalize();
        uploadFileVO.setFilepath(filePath.toString());

        try {
            Files.copy(file.getInputStream(), storePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件存储失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        return ResponseResult.success(uploadFileVO);
    }

    @PostMapping("/uploadZip/**")
    public Result<UploadFileVO> uploadFileZip(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 1. 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            log.warn("上传失败，文件类型不被允许: {}", contentType);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传图片类型文件");
        }

        // 2. 校验文件扩展名（进一步确认）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches("(?i)^.+\\.(zip)$")) {
            log.warn("上传失败，文件扩展名不合法: {}", originalFilename);
            return ResponseResult.failure(ResultCode.FILE_TYPE_NOT_ALLOWED, "仅支持上传文件（.zip）");
        }

        // 获取路径
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setName(originalFilename);
        uploadFileVO.setType(contentType);

        Path resPath = FileUtils.resPath;
        uploadFileVO.setType(contextPath.substring(contextPath.indexOf("upload") + "uploadZip/".length()));
        Path folder = Paths.get(uploadFileVO.getType());
        try {
            Files.createDirectories(resPath.resolve(folder).normalize());
        } catch (IOException e) {
            log.error("创建upload文件夹失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        String ext = null;
        String oldFileName = null;
        try {
            ext = FileUtil.getSuffix(originalFilename);
            oldFileName = FileUtil.getPrefix(originalFilename);
        } catch (Exception e2) {
            log.error("根据文件名获取文件后缀失败", e2);
        }

        String fileName = StringUtils.cleanPath(oldFileName + "-" + System.currentTimeMillis() + "." + ext);
        if (fileName.contains("..")) {
            log.error("文件名不能包含.. {}", fileName);
            return ResponseResult.failure(ResultCode.FILENAME_BAD_REQUEST);
        }

        Path filePath = folder.resolve(fileName).normalize();
        Path storePath = resPath.resolve(filePath).normalize();
        uploadFileVO.setFilepath(filePath.toString());

        try {
            Files.copy(file.getInputStream(), storePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件存储失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        return ResponseResult.success(uploadFileVO);
    }

    @NoLogin
    @GetMapping("/download/**")
    public void downloadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        String fileName = contextPath.substring(contextPath.lastIndexOf("/") + 1);

        fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

        Path resPath = FileUtils.resPath;
        Path filePath = Paths.get(URLDecoder.decode(contextPath.substring(contextPath.indexOf("download/") + "download/".length()), StandardCharsets.UTF_8));
        Path storePath = resPath.resolve(filePath).toAbsolutePath().normalize();

        response.reset();

        File file = resPath.resolve(storePath).toAbsolutePath().toFile();
        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            response.flushBuffer();
            return;
        }

//        if (request.getHeader("If-None-Match") != null) {
//            response.setStatus(304);
//            response.flushBuffer();
//            return;
//        }


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

    @NoLogin
    @GetMapping("/downloadGraphCode/**")
    public void downloadGraphCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        String fileName = contextPath.substring(contextPath.lastIndexOf("/") + 1);

        Path resPath = CaptchaUtils.getCaptchaImagePath();
        Path filePath = Paths.get(contextPath.substring(contextPath.indexOf("downloadGraphCode/") + "downloadGraphCode/".length()));
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

    @GetMapping("/part/download/**")
    public void downloadFileByPart(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    /**
     * 下载支持断点续传
     * @param request
     * @param response
     * @throws IOException
     */
    @GetMapping("/part2/download/**")
    public void downloadFileByPart2(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        Path resPath = FileUtils.resPath;
        Path filePath = Paths.get(contextPath.substring(contextPath.indexOf("download/") + "download/".length()));
        Path storePath = resPath.resolve(filePath).toAbsolutePath().normalize();
        // Get your file stream from wherever.

        File downloadFile = new File(storePath.toString());

        ServletContext context = request.getServletContext();
        // get MIME type of the file
        String mimeType = context.getMimeType(storePath.toString());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        response.setContentType(mimeType);

        // set headers for the response
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
        response.setHeader(headerKey, headerValue);
        // 解析断点续传相关信息
        response.setHeader("Accept-Ranges", "bytes");
        long downloadSize = downloadFile.length();
        long fromPos = 0, toPos = 0;
        if (request.getHeader("Range") == null) {
            response.setHeader("Content-Length", downloadSize + "");
        } else {
            // 若客户端传来Range，说明之前下载了一部分，设置206状态(SC_PARTIAL_CONTENT)
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            String range = request.getHeader("Range");
            String bytes = range.replaceAll("bytes=", "");
            String[] ary = bytes.split("-");
            fromPos = Long.parseLong(ary[0]);
            if (ary.length == 2) {
                toPos = Long.parseLong(ary[1]);
            }
            int size;
            if (toPos > fromPos) {
                size = (int) (toPos - fromPos);
            } else {
                size = (int) (downloadSize - fromPos);
            }
            response.setHeader("Content-Length", size + "");
            String contentRange = "bytes " + fromPos + "-" + toPos + "/" + downloadFile.length();
            response.setHeader(HttpHeaders.CONTENT_RANGE, contentRange);
            downloadSize = size;
        }
        // Copy the stream to the response's output stream.
        RandomAccessFile in = null;
        OutputStream out = null;
        try {
            in = new RandomAccessFile(downloadFile, "rw");
            // 设置下载起始位置
            if (fromPos > 0) {
                in.seek(fromPos);
            }
            // 缓冲区大小
            int bufLen = (int) (downloadSize < 2048 ? downloadSize : 2048);
            byte[] buffer = new byte[bufLen];
            int num;
            int count = 0; // 当前写到客户端的大小
            out = response.getOutputStream();
            while ((num = in.read(buffer)) != -1) {
                out.write(buffer, 0, num);
                count += num;
                //处理最后一段，计算不满缓冲区的大小
                if (downloadSize - count < bufLen) {
                    bufLen = (int) (downloadSize-count);
                    if(bufLen==0){
                        break;
                    }
                    buffer = new byte[bufLen];
                }
            }
            response.flushBuffer();
        } catch (IOException e) {
//            e.printStackTrace();
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
        }
    }

    @PostMapping("/sync/**")
    public Result<UploadFileVO> syncFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String contextPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        contextPath = contextPath.replaceAll("\\.\\.", "");

        UploadFileVO uploadFileVO = new UploadFileVO();
        uploadFileVO.setName(file.getOriginalFilename());
        uploadFileVO.setType(file.getContentType());

        Path resPath = FileUtils.resPath;
        uploadFileVO.setType(contextPath.substring(contextPath.indexOf("sync") + "sync/".length()));
        Path folder = Paths.get(uploadFileVO.getType());
        try {
            Files.createDirectories(resPath.resolve(folder).normalize());
        } catch (IOException e) {
            log.error("创建upload文件夹失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        String fileName = file.getOriginalFilename();
        if (fileName.contains("..")) {
            log.error("文件名不能包含.. {}", fileName);
            return ResponseResult.failure(ResultCode.FILENAME_BAD_REQUEST);
        }
        Path filePath = folder.resolve(fileName).normalize();
        Path storePath = resPath.resolve(filePath).normalize();
        uploadFileVO.setFilepath(filePath.toString());

        try {
            Files.copy(file.getInputStream(), storePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件存储失败", e);
            return ResponseResult.failure(ResultCode.INTERNAL_SERVER_ERROR);
        }

        return ResponseResult.success(uploadFileVO);
    }

}
