package com.nyy.gmail.cloud.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtils {
//    /**
//     * 替换模板中的变量占位符，如 {{var | 默认值}}，优先用 values 中的值，否则用默认值。
//     */
//    public static String replaceTemplateVars(String template, Map<String, String> values) {
//        if (template == null || template.isEmpty()) return template;
//
//        Pattern pattern = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\|\\s*([^}]+)\\s*}}");
//        Matcher matcher = pattern.matcher(template);
//        StringBuffer sb = new StringBuffer();
//
//        while (matcher.find()) {
//            String varName = matcher.group(1).trim();
//            String defaultValue = matcher.group(2).trim();
//
//            String replacement = values.getOrDefault(varName, defaultValue);
//            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
//        }
//        matcher.appendTail(sb);
//
//        return sb.toString();
//    }

    /**
     * 替换模板中的变量占位符，如 {{var | 默认值}} 或 {{var}}。
     * 优先用 values 中的值，否则用默认值；都没有时保留原样。
     */
    public static String replaceTemplateVars(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) return template;

        // 支持 {{var}} 和 {{var | 默认值}}
        Pattern pattern = Pattern.compile("\\{\\{\\s*(\\w+)\\s*(?:\\|\\s*([^}]+))?\\s*}}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String defaultValue = matcher.group(2) != null ? matcher.group(2).trim() : null;

            String replacement;
            if (values.containsKey(varName)) {
                replacement = values.get(varName);
            } else if (defaultValue != null) {
                replacement = defaultValue;
            } else {
                // 保留原始 {{...}}
                replacement = matcher.group(0);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static List<String> extractLinks(String html) {
        List<String> links = new ArrayList<>();

        // 正则匹配 <a href="...">
        Pattern pattern = Pattern.compile("<a\\s+[^>]*?href\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            links.add(matcher.group(1));
        }

        return links;
    }

    public static List<String> extractImgSrc(String html) {
        List<String> srcList = new ArrayList<>();
        Pattern pattern = Pattern.compile("<img\\s+[^>]*src\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            srcList.add(matcher.group(1));
        }

        return srcList;
    }

    public static String saveBase64Image(String base64Src, File saveDir) throws IOException {
        if (base64Src == null || !base64Src.contains(",")) return "";

        String[] parts = base64Src.split(",");
        if (parts.length != 2) return "";

        // 确保保存目录存在
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + saveDir.getAbsolutePath());
            }
        }

        if (!saveDir.canWrite()) {
            throw new IOException("No write permission for directory: " + saveDir.getAbsolutePath());
        }

        String mimeType = parts[0]; // data:image/png;base64
        String base64Data = parts[1];

        // 识别文件扩展名
        String extension = "png"; // 默认扩展名
        if (mimeType.contains("jpeg")) extension = "jpg";
        else if (mimeType.contains("gif")) extension = "gif";
        else if (mimeType.contains("webp")) extension = "webp";

        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        String fileName = UUID.randomUUID() + "." + extension;
        File outFile = new File(saveDir, fileName);

        if (extension.equals("webp") || imageBytes.length > 1024 * 1024) {
            // WebP 直接以二进制方式保存
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(imageBytes);
            }
            return fileName;
        }

        // 解码为图像
        BufferedImage image;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            image = ImageIO.read(bais);
            if (image == null) throw new IOException("Invalid image data (ImageIO.read returned null)");
        }

        // 修改一个随机像素点，避免重复图像 hash
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 0 && height > 0) {
            Random rand = new Random();
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            image.setRGB(x, y, randomColor.getRGB());
        }

        // 保存图像
        String format = extension.equals("jpg") ? "jpeg" : extension;
        boolean success = ImageIO.write(image, format, outFile);
        if (!success) {
            throw new IOException("Image format not supported for writing: " + format);
        }

        return fileName;
    }

    public static String saveNetworkImage(String imageUrl, File saveDir) throws IOException {
        URL url = new URL(imageUrl);
        String fileName = UUID.randomUUID().toString();

        // 确保保存目录存在
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + saveDir.getAbsolutePath());
            }
        }

        if (!saveDir.canWrite()) {
            throw new IOException("No write permission for directory: " + saveDir.getAbsolutePath());
        }

        String extension = "jpg"; // 默认
        String path = url.getPath();
        int idx = path.lastIndexOf(".");
        if (idx != -1) {
            extension = path.substring(idx + 1).toLowerCase();
        }

        if (extension.equals("webp")) {
            File outFile = new File(saveDir, fileName + "." + extension);
            try (InputStream in = url.openStream()) {
                Files.copy(in, outFile.toPath());
            }
            return fileName + "." + extension;
        }

        // 读取网络图片为 BufferedImage
        BufferedImage image;
        long fileSize;

        try (InputStream in = url.openStream()) {
            // 获取图片的字节流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            // 获取字节数，即文件大小
            fileSize = byteArrayOutputStream.size();

            // 读取图片数据
            image = ImageIO.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            if (image == null) {
                throw new IOException("无法读取网络图片，可能不是支持的格式");
            }
        }

        // 修改一个随机像素为随机颜色
        if (fileSize <= 1024 * 1024) {
            int width = image.getWidth();
            int height = image.getHeight();
            if (width > 0 && height > 0) {
                Random rand = new Random();
                int x = rand.nextInt(width);
                int y = rand.nextInt(height);
                Color randomColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
                image.setRGB(x, y, randomColor.getRGB());
            }
        }

        // 保存修改后的图片
        String finalFileName = fileName + "." + extension;
        File outFile = new File(saveDir, finalFileName);

        // 注意：ImageIO 默认不支持 webp 写出
        boolean result = ImageIO.write(image, extension.equals("jpg") ? "jpeg" : extension, outFile);
        if (!result) throw new IOException("ImageIO 不支持保存格式：" + extension);

        return finalFileName;
    }

    public static void main(String[] args) {
//        System.out.println(replaceTemplateVars("Hello {{var1 | zhangsan}}, Welcome {{var2 | Beijing}}!", new HashMap<>(Map.of("var1", "lisi"))));
//        String html = """
//            <div>
//                <img src="https://example.com/image1.jpg" alt="img1" />
//                <img src="https://openai.com/logo.png"/>
//            </div>
//        """;
//
//        List<String> imgSources = extractImgSrc(html);
//        imgSources.forEach(System.out::println);

//        String html = """
//            <p>Click <a href="https://example.com">here</a> or visit <a href="https://openai.com">OpenAI</a></p>
//        """;
//
//        List<String> links = extractLinks(html);
//        links.forEach(System.out::println);
    }
}
