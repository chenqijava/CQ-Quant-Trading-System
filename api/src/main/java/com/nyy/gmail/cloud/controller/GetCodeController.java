package com.nyy.gmail.cloud.controller;

import com.nyy.gmail.cloud.common.Session;
import com.nyy.gmail.cloud.common.annotation.NoLogin;
import com.nyy.gmail.cloud.common.exception.CommonException;
import com.nyy.gmail.cloud.common.response.ResponseResult;
import com.nyy.gmail.cloud.common.response.Result;
import com.nyy.gmail.cloud.common.response.ResultCode;
import com.nyy.gmail.cloud.entity.mongo.SendEmailEventMonitor;
import com.nyy.gmail.cloud.entity.mongo.SendEmailEventTracking;
import com.nyy.gmail.cloud.entity.mongo.SubTask;
import com.nyy.gmail.cloud.model.dto.AccountListDTO;
import com.nyy.gmail.cloud.repository.mongo.SendEmailEventMonitorRepository;
import com.nyy.gmail.cloud.repository.mongo.SendEmailEventTrackingRepository;
import com.nyy.gmail.cloud.repository.mongo.SubTaskRepository;
import com.nyy.gmail.cloud.service.GetCodeService;
import com.nyy.gmail.cloud.utils.FileUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import org.yaml.snakeyaml.util.UriEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping({"/api/latest" })
public class GetCodeController {

    @Autowired
    private GetCodeService getCodeService;

    @Autowired
    private SendEmailEventMonitorRepository sendEmailEventMonitorRepository;

    @Autowired
    private SendEmailEventTrackingRepository sendEmailEventTrackingRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    @NoLogin
    @GetMapping("code")
    public Result<Map<String, String>> code(@RequestParam(required = true, name = "id") String id, @RequestParam(required = true, name = "aid") String aid) {
        if (StringUtils.isEmpty(id)) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        if (StringUtils.isEmpty(aid)) {
            throw new CommonException(ResultCode.PARAMS_IS_INVALID);
        }
        Map<String, String> msg = getCodeService.getCode(id, aid);
        return ResponseResult.success(msg);
    }

    private static String unsubscribeHTML = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <title>Unsubscription Successful</title>\n" +
            "  <style>\n" +
            "    body {\n" +
            "      font-family: Arial, sans-serif;\n" +
            "      background-color: #ffffff;\n" +
            "      color: #333333;\n" +
            "      padding: 50px;\n" +
            "    }\n" +
            "    .container {\n" +
            "      max-width: 600px;\n" +
            "      margin: auto;\n" +
            "      text-align: left;\n" +
            "    }\n" +
            "    h1 {\n" +
            "      font-size: 20px;\n" +
            "      font-weight: bold;\n" +
            "    }\n" +
            "    p {\n" +
            "      margin-top: 20px;\n" +
            "      font-size: 14px;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"container\">\n" +
            "    <h1>Unsubscription successful</h1>\n" +
            "    <p>You have successfully unsubscribed and will no longer receive our emails. Thank you for your attention!</p>\n" +
            "  </div>\n" +
            "</body>\n" +
            "</html>\n";

    @NoLogin
    @GetMapping("external/{id}/{key}")
    public void external(@PathVariable String id, @PathVariable String key, HttpServletResponse response) throws IOException {
        SendEmailEventMonitor one = sendEmailEventMonitorRepository.findOneByUUID(id, "click");
        if (one == null || one.getLinkUrls() == null || !one.getLinkUrls().containsKey(key)) {
            if (key.equals("unsubscribe")) {
                if (one != null) {
                    saveTracking(one, Map.of("key", key, "id", id));
                }

                response.setContentType("text/html; charset=UTF-8");

                PrintWriter out = response.getWriter();
                out.println(unsubscribeHTML);
                return;
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            saveTracking(one, Map.of("key", key, "id", id));
            String link = one.getLinkUrls().get(key);
            response.setStatus(HttpServletResponse.SC_FOUND); // 302
            response.setHeader("Location", link);
        }
    }

    private void saveTracking(SendEmailEventMonitor one, Map<String, String> map) {
        try {
            if (one.getCount() == null) {
                one.setCount(0);
            }
            one.setCount(one.getCount() + 1);
            sendEmailEventMonitorRepository.update(one);

            SendEmailEventTracking sendEmailEventTracking = new SendEmailEventTracking();
            sendEmailEventTracking.setEvent(one.getEvent());
            sendEmailEventTracking.setEmail(one.getEmail());
            sendEmailEventTracking.setCreateTime(new Date());
            sendEmailEventTracking.setParams(map);
            sendEmailEventTracking.setGroupTaskId(one.getGroupTaskId());
            sendEmailEventTracking.setSubTaskId(one.getSubTaskId());
            sendEmailEventTracking.setUserID(one.getUserID());

            sendEmailEventTrackingRepository.save(sendEmailEventTracking);

            SubTask subTask = subTaskRepository.findById(one.getSubTaskId());
            if (subTask != null) {
                subTaskRepository.updateEvent(subTask.get_id(), one.getEvent(), "1");
            }
        } catch (Exception e) {}
    }

    @NoLogin
    @GetMapping("/img/{id}/{name}")
    public void downloadFile(@PathVariable String id, @PathVariable String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = name;
        SendEmailEventMonitor one = sendEmailEventMonitorRepository.findOneByUUID(id, "open");
        if (one != null) {
            saveTracking(one, Map.of("name", name, "id", id));
        }

        if (name.equals("logo.png")) {
            // 生成随机颜色
            Random rand = new Random();

            // 随机起始颜色和结束颜色
            Color startColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            Color endColor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));

            // 创建 200x80 的图像（比 10x10 大一点，渐变才明显）
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // 开启抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 随机渐变方向：水平/垂直/对角
            int direction = rand.nextInt(3);
            GradientPaint paint;
            switch (direction) {
                case 0: // 左到右
                    paint = new GradientPaint(0, 0, startColor, img.getWidth(), 0, endColor);
                    break;
                case 1: // 上到下
                    paint = new GradientPaint(0, 0, startColor, 0, img.getHeight(), endColor);
                    break;
                default: // 左上到右下
                    paint = new GradientPaint(0, 0, startColor, img.getWidth(), img.getHeight(), endColor);
                    break;
            }

            g.setPaint(paint);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.dispose();

            // 设置响应头
            response.setContentType("image/jpeg");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");

            // 写入图片
            ImageIO.write(img, "jpg", response.getOutputStream());
            return;
        }

        Path resPath = FileUtils.resPath;
        Path filePath = Paths.get("img",  fileName);
        Path storePath = resPath.resolve(filePath).toAbsolutePath().normalize();

        response.reset();

        File file = resPath.resolve(storePath).toAbsolutePath().toFile();
        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            response.flushBuffer();
            return;
        }

        // 假设你返回的是 JPEG 图片
        response.setContentType("image/jpeg");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength((int) file.length());

        // 不要设置 Content-Disposition 下载头
        // response.setHeader("Content-Disposition", "attachment;filename=" + UriEncoder.encode(fileName));

        response.setHeader("Cache-Control", "public");
        response.setHeader("ETag", filePath.toString());

        // 输出文件内容
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }
    }
}
