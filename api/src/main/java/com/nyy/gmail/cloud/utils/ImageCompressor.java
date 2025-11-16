package com.nyy.gmail.cloud.utils;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class ImageCompressor {

    /**
     * 压缩并重写图片，去除元数据，防止HTML/脚本注入。
     *
     * @param inputStream 原始图片输入流
     * @param outputPath 存储路径（含文件名）
     * @throws IOException 压缩失败
     */
    public static void compressAndClean(InputStream inputStream, Path outputPath) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("无法读取图像内容，可能不是有效图片");
        }

        // 重新写入图片，覆盖潜在恶意信息
        Thumbnails.of(image)
                .scale(1.0)                   // 保持原尺寸
                .outputQuality(0.85)          // 压缩质量（85%）
                .outputFormat("jpg")          // 强制转换为JPG，移除可能的脚本（例如SVG、GIF动画）
                .toFile(outputPath.toFile());
    }
}
