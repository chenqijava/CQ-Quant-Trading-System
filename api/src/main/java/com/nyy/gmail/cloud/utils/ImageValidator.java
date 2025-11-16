package com.nyy.gmail.cloud.utils;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ImageValidator {

    /**
     * 下载并验证 URL 是否是合法图片，如果 valid 则返回 BufferedImage。
     * @param imageUrl  图片链接
     * @return BufferedImage 对象
     * @throws IOException 下载或验证失败时抛出
     */
    public static byte[] downloadAndValidateImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

        String contentType = connection.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("不是图片类型: " + contentType);
        }

        try (InputStream inputStream = connection.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("文件无法解析为有效图片");
            }
            return bufferedImageToBytes(image, "jpg");
        }
    }

    /**
     * 将 BufferedImage 转为字节数组
     * @param image BufferedImage 对象
     * @param format 格式，如 "jpg"、"png"
     * @return 图片的字节数组
     * @throws IOException 写入失败
     */
    public static byte[] bufferedImageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean success = ImageIO.write(image, format, baos);
        if (!success) {
            throw new IOException("ImageIO 不支持格式: " + format);
        }
        return baos.toByteArray();
    }
}
