package com.nyy.gmail.cloud.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FileUtilsHelper {

    /**
     * 读取文件的所有行，以 List<String> 返回
     */
    public static List<String> readAllLines(File file) {
        return FileUtil.readUtf8Lines(file);
    }

    /**
     * 逐行读取文件，返回 BufferedReader（让外部自己循环）
     */
    public static BufferedReader getReader(File file) {
        return FileUtil.getReader(file, "UTF-8");
    }

    /**
     * 追加一行或多行到文件（UTF-8）
     */
    public static void appendLines(List<String> lines, File file) {
        FileUtil.appendUtf8Lines(lines, file);
    }

    public static void appendLine(String line, File file) {
        FileUtil.appendUtf8String(line + "\n", file);
    }

    /**
     * 覆盖写字符串到文件
     */
    public static void writeString(String content, File file) {
        FileUtil.writeUtf8String(content, file);
    }

    /**
     * 移动文件
     */
    public static void moveFile(File src, File dest, boolean isOverride) {
        FileUtil.move(src, dest, isOverride);
    }

    /**
     * 创建一个临时文件
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix).toFile();
    }
}
