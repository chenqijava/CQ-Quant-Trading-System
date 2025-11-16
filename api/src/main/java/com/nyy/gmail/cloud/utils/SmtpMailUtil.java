package com.nyy.gmail.cloud.utils;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class SmtpMailUtil {

    private static final List<String> Junk_Folder_List = List.of("junk", "spam");

    @Data
    public static class MailMessage {
        private String id;
        private String subject;
        private String from;
        private String[] to;
        private Date sentDate;
        private String textContent; // 纯文本正文
        private String htmlContent; // HTML正文
        private List<String> attachments = new ArrayList<>(); // 附件文件名

        public void addAttachment(String attachment) { this.attachments.add(attachment); }

        @Override
        public String toString() {
            return "MailMessage{" +
                    "subject='" + subject + '\'' +
                    ", from='" + from + '\'' +
                    ", sentDate=" + sentDate +
                    ", textContent='" + textContent + '\'' +
                    ", htmlContent='" + htmlContent + '\'' +
                    ", attachments=" + attachments +
                    '}';
        }
    }

    /**
     * 发送邮件（支持 HTML / 附件）
     */
    public static void sendMail(String smtpHost, int smtpPort,
                                final String username, final String password,
                                String from, List<String> to,
                                String subject, String content,
                                boolean html, File attachment) throws Exception {

        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS
        props.put("mail.smtp.ssl.trust", smtpHost);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        InternetAddress[] recipientList =  new InternetAddress[to.size()];
        for (int i = 0; i < to.size(); i++) {
            recipientList[i] = new InternetAddress(to.get(i));
        }
        message.setRecipients(Message.RecipientType.TO, recipientList);
        message.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        // 正文部分
        MimeBodyPart textPart = new MimeBodyPart();
        if (html) {
            textPart.setContent(content, "text/html;charset=UTF-8");
        } else {
            textPart.setText(content, "UTF-8");
        }
        multipart.addBodyPart(textPart);

        // 附件部分
        if (attachment != null && attachment.exists()) {
            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));
            attachPart.setFileName(attachment.getName());
            multipart.addBodyPart(attachPart);
        }

        message.setContent(multipart);

        Transport.send(message);
        System.out.println("✅ 邮件发送成功！");
    }

    private static String extractEmail(String address) {
        String email = address;
        int start = address.indexOf('<');
        int end = address.indexOf('>');
        if (start != -1 && end != -1) {
            email = address.substring(start + 1, end);
        }
        return email;
    }

    public static List<MailMessage> receiveMail(String imapHost, int imapPort,
                                                final String username, final String password,
                                                boolean ssl, int limit, String folder) throws Exception {
        if (limit <= 0) {
            limit = 10000;
        }
        List<MailMessage> mailList = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        props.put("mail.imap.ssl.enable", String.valueOf(ssl));

        Session session = Session.getInstance(props);
        Store store = session.getStore("imap");
        store.connect(imapHost, username, password);

        Folder inbox = store.getFolder(folder);
        inbox.open(Folder.READ_ONLY);

        int total = inbox.getMessageCount();
        int start = Math.max(1, total - limit + 1);
        Message[] messages = inbox.getMessages(start, total);

        for (int i = messages.length - 1; i >= 0; i--) {
            Message msg = messages[i];
            MailMessage mail = new MailMessage();
            mail.setSubject(msg.getSubject());
            mail.setFrom(extractEmail(msg.getFrom()[0].toString()));
            Address[] recipients = msg.getAllRecipients();
            if (recipients != null) {
                String[] tos = new String[recipients.length];
                for (int j = 0; j < recipients.length; j++) {
                    tos[j] = recipients[j].toString();
                }
                mail.setTo(tos);
            }
            mail.setSentDate(msg.getSentDate());

            // 获取正文
            TextAndHtml textAndHtml = getTextAndHtmlFromMessage(msg);
            mail.setTextContent(textAndHtml.text);
            mail.setHtmlContent(textAndHtml.html);

            mail.setId(calcId(mail));
            // 附件
            if (msg.getContent() instanceof Multipart) {
                Multipart multipart = (Multipart) msg.getContent();
                for (int j = 0; j < multipart.getCount(); j++) {
                    BodyPart part = multipart.getBodyPart(j);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        mail.addAttachment(part.getFileName());
                    }
                }
            }

            mailList.add(mail);
        }

        inbox.close(false);
        store.close();

        return mailList;
    }

    private static String calcId(MailMessage mail) {
        String content = StringUtils.isEmpty(mail.getHtmlContent()) ? mail.getTextContent() : mail.getHtmlContent();
        String timestamp = mail.getSentDate().toString();
        return MD5Util.MD5(mail.getFrom() + String.join(",", mail.getTo()) + content + timestamp);
    }

    public static List<MailMessage> receiveInboxMail(String imapHost, int imapPort,
                                                final String username, final String password,
                                                boolean ssl, int limit) throws Exception {
        return receiveMail(imapHost, imapPort, username, password, ssl, limit, "INBOX");
    }

    public static List<MailMessage> receiveSpamMail(String imapHost, int imapPort,
                                                final String username, final String password,
                                                boolean ssl, int limit) throws Exception {
        List<String> allFolders = getAllFolders(imapHost, imapPort, username, password, ssl);
        for (String folder : allFolders) {
            if (Junk_Folder_List.stream().anyMatch(e -> folder.toLowerCase().contains(e.toLowerCase()))) {
                return receiveMail(imapHost, imapPort, username, password, ssl, limit, folder);
            }
        }
        return new ArrayList<>();
    }

    // 用于返回文本和HTML
    private static class TextAndHtml {
        String text = "";
        String html = "";
    }

    // 提取正文内容，同时获取纯文本和HTML
    private static TextAndHtml getTextAndHtmlFromMessage(Message message) throws Exception {
        Object content = message.getContent();
        TextAndHtml result = new TextAndHtml();

        if (content instanceof String) {
            String str = (String) content;
            if (message.isMimeType("text/html")) {
                result.html = str;
            } else {
                result.text = str;
            }
        } else if (content instanceof Multipart) {
            extractFromMultipart((Multipart) content, result);
        }
        return result;
    }

    // 递归解析Multipart
    private static void extractFromMultipart(Multipart multipart, TextAndHtml result) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain") && result.text.isEmpty()) {
                result.text = (String) part.getContent();
            } else if (part.isMimeType("text/html") && result.html.isEmpty()) {
                result.html = (String) part.getContent();
            } else if (part.getContent() instanceof Multipart) {
                extractFromMultipart((Multipart) part.getContent(), result);
            }
        }
    }

    /**
     * 获取邮箱中所有文件夹（递归）
     */
    public static List<String> getAllFolders(String imapHost, int imapPort,
                                             final String username, final String password,
                                             boolean ssl) throws Exception {
        List<String> folderList = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        props.put("mail.imap.ssl.enable", String.valueOf(ssl));

        Session session = Session.getInstance(props);
        Store store = session.getStore("imap");
        store.connect(imapHost, username, password);

        Folder defaultFolder = store.getDefaultFolder();
        listFoldersRecursive(defaultFolder, folderList);

        store.close();
        return folderList;
    }

    /**
     * 递归遍历所有文件夹
     */
    private static void listFoldersRecursive(Folder folder, List<String> folderList) throws Exception {
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            folderList.add(folder.getFullName());
        }
        if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
            for (Folder subFolder : folder.list()) {
                listFoldersRecursive(subFolder, folderList);
            }
        }
    }

    // 简单测试
    public static void main(String[] args) throws Exception {
        // 示例：发送邮件
        // mail.itntmail.com----25

//        sendMail(
//                "mail.itntmail.com",
//                25,
//                "jenney865@itntmail.com",
//                "80,CaLIOn,#",
//                "jenney865@itntmail.com",
//                "1311949161@qq.com",
//                "测试邮件",
//                "<b>Hello World!</b>",
//                true,
//                null
//        );

        // 示例：接收邮件
//        receiveMail(
//                "mail.itntmail.com",
//                993,
//                "jenney865@itntmail.com",
//                "80,CaLIOn,#",
//                true,
//                5
//        );

//        List<String> folders = getAllFolders(
//                "mail.itntmail.com",
//                993,
//                "jenney865@itntmail.com",
//                "80,CaLIOn,#",
//                true
//        );
//
//        folders.forEach(System.out::println);
//
        receiveInboxMail(
                "mail.itntmail.com",
                993,
                "jenney865@itntmail.com",
                "80,CaLIOn,#",
                true,
                5);
//
//        receiveSpamMail(
//                "mail.itntmail.com",
//                993,
//                "jenney865@itntmail.com",
//                "80,CaLIOn,#",
//                true,
//                5);
    }
}

