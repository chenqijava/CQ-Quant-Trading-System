package com.nyy.gmail.cloud.utils.tgnet;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class User {
    // 基本属性
    public long id;
    public Boolean isSelf;
    public Boolean contact;
    public Boolean mutualContact;
    public Boolean deleted;
    public Boolean bot;
    public Boolean botChatHistory;
    public Boolean botNochats;
    public Boolean verified;
    public Boolean restricted;
    public Boolean min;
    public Boolean botInlineGeo;
    public Boolean support;
    public Boolean scam;
    public Boolean applyMinPhoto;
    public Boolean fake;
    public Boolean botAttachMenu;
    public Boolean premium;
    public Boolean attachMenuEnabled;
    public Boolean botCanEdit;
    public Boolean closeFriend;
    public Boolean storiesHidden;
    public Boolean storiesUnavailable;
    public Boolean contactRequirePremium;
    public Boolean botBusiness;
    public Boolean botHasMainApp;
    public Boolean botForumView;
    public Long accessHash;
    public String firstName;
    public String lastName;
    public String username;
    public String phone;
    public Object photo;
    public Object status;
    public Integer botInfoVersion;
    public List<Object> restrictionReason;
    public String botInlinePlaceholder;
    public String langCode;
    public Object emojiStatus;
    public List<Object> usernames;
    public Integer storiesMaxId;
    public Object color;
    public Object profileColor;
    public Integer botActiveUsers;
    public Long botVerificationIcon;
    public Long sendPaidMessagesStars;

    // 构造函数
    public User(long id, Boolean isSelf, Boolean contact, Boolean mutualContact, Boolean deleted,
                Boolean bot, Boolean botChatHistory, Boolean botNochats, Boolean verified,
                Boolean restricted, Boolean min, Boolean botInlineGeo, Boolean support,
                Boolean scam, Boolean applyMinPhoto, Boolean fake, Boolean botAttachMenu,
                Boolean premium, Boolean attachMenuEnabled, Boolean botCanEdit,
                Boolean closeFriend, Boolean storiesHidden, Boolean storiesUnavailable,
                Boolean contactRequirePremium, Boolean botBusiness, Boolean botHasMainApp,
                Boolean botForumView, Long accessHash, String firstName, String lastName,
                String username, String phone, Object photo, Object status,
                Integer botInfoVersion, List<Object> restrictionReason,
                String botInlinePlaceholder, String langCode, Object emojiStatus,
                List<Object> usernames, Integer storiesMaxId, Object color,
                Object profileColor, Integer botActiveUsers, Long botVerificationIcon,
                Long sendPaidMessagesStars) {
        this.id = id;
        this.isSelf = isSelf;
        this.contact = contact;
        this.mutualContact = mutualContact;
        this.deleted = deleted;
        this.bot = bot;
        this.botChatHistory = botChatHistory;
        this.botNochats = botNochats;
        this.verified = verified;
        this.restricted = restricted;
        this.min = min;
        this.botInlineGeo = botInlineGeo;
        this.support = support;
        this.scam = scam;
        this.applyMinPhoto = applyMinPhoto;
        this.fake = fake;
        this.botAttachMenu = botAttachMenu;
        this.premium = premium;
        this.attachMenuEnabled = attachMenuEnabled;
        this.botCanEdit = botCanEdit;
        this.closeFriend = closeFriend;
        this.storiesHidden = storiesHidden;
        this.storiesUnavailable = storiesUnavailable;
        this.contactRequirePremium = contactRequirePremium;
        this.botBusiness = botBusiness;
        this.botHasMainApp = botHasMainApp;
        this.botForumView = botForumView;
        this.accessHash = accessHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.phone = phone;
        this.photo = photo;
        this.status = status;
        this.botInfoVersion = botInfoVersion;
        this.restrictionReason = restrictionReason;
        this.botInlinePlaceholder = botInlinePlaceholder;
        this.langCode = langCode;
        this.emojiStatus = emojiStatus;
        this.usernames = usernames;
        this.storiesMaxId = storiesMaxId;
        this.color = color;
        this.profileColor = profileColor;
        this.botActiveUsers = botActiveUsers;
        this.botVerificationIcon = botVerificationIcon;
        this.sendPaidMessagesStars = sendPaidMessagesStars;
    }

    // fromReader静态方法
    // Todo 暂时没有完全实现
    public static User fromReader(DataInputStream reader) throws IOException {
        int flags = reader.readInt();

        boolean isSelf = (flags & 1024) != 0;
        boolean contact = (flags & 2048) != 0;
        boolean mutualContact = (flags & 4096) != 0;
        boolean deleted = (flags & 8192) != 0;
        boolean bot = (flags & 16384) != 0;
        boolean botChatHistory = (flags & 32768) != 0;
        boolean botNochats = (flags & 65536) != 0;
        boolean verified = (flags & 131072) != 0;
        boolean restricted = (flags & 262144) != 0;
        boolean min = (flags & 1048576) != 0;
        boolean botInlineGeo = (flags & 2097152) != 0;
        boolean support = (flags & 8388608) != 0;
        boolean scam = (flags & 16777216) != 0;
        boolean applyMinPhoto = (flags & 33554432) != 0;
        boolean fake = (flags & 67108864) != 0;
        boolean botAttachMenu = (flags & 134217728) != 0;
        boolean premium = (flags & 268435456) != 0;
        boolean attachMenuEnabled = (flags & 536870912) != 0;

        int flags2 = reader.readInt();

        boolean botCanEdit = (flags2 & 2) != 0;
        boolean closeFriend = (flags2 & 4) != 0;
        boolean storiesHidden = (flags2 & 8) != 0;
        boolean storiesUnavailable = (flags2 & 16) != 0;
        boolean contactRequirePremium = (flags2 & 1024) != 0;
        boolean botBusiness = (flags2 & 2048) != 0;
        boolean botHasMainApp = (flags2 & 8192) != 0;
        boolean botForumView = (flags2 & 65536) != 0;

        long id = reader.readLong();

        Long accessHash = null;
        if ((flags & 1) != 0) {
            accessHash = reader.readLong();
        }

        String firstName = null;
        if ((flags & 2) != 0) {
            firstName = readString(reader);
        }

        String lastName = null;
        if ((flags & 4) != 0) {
            lastName = readString(reader);
        }

        String username = null;
        if ((flags & 8) != 0) {
            username = readString(reader);
        }

        String phone = null;
        if ((flags & 16) != 0) {
            phone = readString(reader);
        }

        Object photo = null;
        if ((flags & 32) != 0) {
            photo = readObject(reader);
        }

        Object status = null;
        if ((flags & 64) != 0) {
            status = readObject(reader);
        }

        Integer botInfoVersion = null;
        if ((flags & 16384) != 0) {
            botInfoVersion = reader.readInt();
        }

        List<Object> restrictionReason = null;
        if ((flags & 262144) != 0) {
            reader.readInt(); // skip
            int restrictionCount = reader.readInt();
            restrictionReason = new java.util.ArrayList<>();
            for (int i = 0; i < restrictionCount; i++) {
                Object x = readObject(reader);
                restrictionReason.add(x);
            }
        }

        String botInlinePlaceholder = null;
        if ((flags & 524288) != 0) {
            botInlinePlaceholder = readString(reader);
        }

        String langCode = null;
        if ((flags & 4194304) != 0) {
            langCode = readString(reader);
        }

        Object emojiStatus = null;
        if ((flags & 1073741824) != 0) {
            emojiStatus = readObject(reader);
        }

        List<Object> usernames = null;
        if ((flags2 & 1) != 0) {
            reader.readInt(); // skip
            int usernameCount = reader.readInt();
            usernames = new java.util.ArrayList<>();
            for (int i = 0; i < usernameCount; i++) {
                Object x = readObject(reader);
                usernames.add(x);
            }
        }

        Integer storiesMaxId = null;
        if ((flags2 & 32) != 0) {
            storiesMaxId = reader.readInt();
        }

        Object color = null;
        if ((flags2 & 256) != 0) {
            color = readObject(reader);
        }

        Object profileColor = null;
        if ((flags2 & 512) != 0) {
            profileColor = readObject(reader);
        }

        Integer botActiveUsers = null;
        if ((flags2 & 4096) != 0) {
            botActiveUsers = reader.readInt();
        }

        Long botVerificationIcon = null;
        if ((flags2 & 16384) != 0) {
            botVerificationIcon = reader.readLong();
        }

        Long sendPaidMessagesStars = null;
        if ((flags2 & 32768) != 0) {
            sendPaidMessagesStars = reader.readLong();
        }

        return new User(
                id, isSelf, contact, mutualContact, deleted, bot, botChatHistory,
                botNochats, verified, restricted, min, botInlineGeo, support, scam,
                applyMinPhoto, fake, botAttachMenu, premium, attachMenuEnabled,
                botCanEdit, closeFriend, storiesHidden, storiesUnavailable,
                contactRequirePremium, botBusiness, botHasMainApp, botForumView,
                accessHash, firstName, lastName, username, phone, photo, status,
                botInfoVersion, restrictionReason, botInlinePlaceholder, langCode,
                emojiStatus, usernames, storiesMaxId, color, profileColor,
                botActiveUsers, botVerificationIcon, sendPaidMessagesStars
        );
    }

    // 辅助方法 - 需要根据Telegram协议实现具体逻辑
// 从 Telegram 二进制数据流中读取字符串数据
    private static String readString(InputStream stream) throws IOException {
        int sl = 1;
        int length = stream.read();

        if (length == 254) {
            sl = 4;
            throw new IOException("String length 254 is not implemented");
        }

        int addition = (length + sl) % 4;
        if (addition != 0) {
            addition = 4 - addition;
        }

        byte[] bytes = new byte[length];
        stream.read(bytes);

        String string;
        try {
            string = new String(bytes, "UTF-8");
        } catch (Exception e) {
            string = new String(bytes);
        }

        // 跳过填充字节
        stream.skip(addition);

        return string;
    }


    private static Object readObject(DataInputStream reader) throws IOException {
        // 实现Telegram对象读取逻辑
        return null;
    }
}

