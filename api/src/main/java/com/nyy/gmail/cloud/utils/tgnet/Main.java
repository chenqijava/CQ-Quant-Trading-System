package com.nyy.gmail.cloud.utils.tgnet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class Main {
    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        String targetDir = "C:\\Users\\Administrator\\Desktop\\test\\_tg_20251014_20";
        String sessionDir = targetDir + "-json";

        // 删除并重新创建输出目录
        try {
            Path sessionPath = Paths.get(sessionDir);
            if (Files.exists(sessionPath)) {
                Files.walk(sessionPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            Files.createDirectories(sessionPath);
        } catch (IOException e) {
            System.err.println("创建目录时出错: " + e.getMessage());
            return;
        }

        // 对数据进行处理，并存储到文件中
        SessionParser.processDirectory(targetDir, sessionDir).join();
    }
}