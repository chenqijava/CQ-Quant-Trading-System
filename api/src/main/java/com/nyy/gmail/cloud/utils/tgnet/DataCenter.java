package com.nyy.gmail.cloud.utils.tgnet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataCenter {
    private int dcId;
    private Integer authorized;
    private Integer lastInitVersion;
    private byte[] authKey;
    private List<Address> addrArray;

    public DataCenter(int dcId) {
        this.dcId = dcId;
        this.addrArray = new ArrayList<>();
    }

    // Getter和Setter方法
    public int getDcId() {
        return dcId;
    }

    public void setDcId(int dcId) {
        this.dcId = dcId;
    }

    public Integer getAuthorized() {
        return authorized;
    }

    public void setAuthorized(Integer authorized) {
        this.authorized = authorized;
    }

    public Integer getLastInitVersion() {
        return lastInitVersion;
    }

    public void setLastInitVersion(Integer lastInitVersion) {
        this.lastInitVersion = lastInitVersion;
    }

    public byte[] getAuthKey() {
        return authKey;
    }

    public void setAuthKey(byte[] authKey) {
        this.authKey = authKey;
    }

    public List<Address> getAddrArray() {
        return addrArray;
    }

    public void setAddrArray(List<Address> addrArray) {
        this.addrArray = addrArray;
    }

    // 在 DataCenter.java 中实现 save 方法
    public Session save(String filePath) throws Exception {
        // 创建文件输出流
        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // 写入数据中心基本信息
            dos.writeInt(this.dcId);
            dos.writeInt(this.lastInitVersion != null ? this.lastInitVersion : 0);
            dos.writeInt(this.authorized != null ? this.authorized : 0);

            // 写入认证密钥
            if (this.authKey != null) {
                dos.writeInt(this.authKey.length);
                dos.write(this.authKey);
            } else {
                dos.writeInt(0);
            }

            // 写入地址数组
            if (this.addrArray != null) {
                dos.writeInt(this.addrArray.size());
                for (Address addr : this.addrArray) {
                    // 写入地址信息
                    writeString(dos, addr.getAddr());
                    dos.writeInt(addr.getPort());
                    dos.writeInt(addr.getFlag());
                    if (addr.getSecret() != null) {
                        dos.writeInt(addr.getSecret().length);
                        dos.write(addr.getSecret());
                    } else {
                        dos.writeInt(0);
                    }
                }
            } else {
                dos.writeInt(0);
            }

            // 返回新的 Session 对象
            return new Session(filePath);
        }
    }

    // 辅助方法：写入字符串
    private void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null) {
            dos.writeInt(0);
        } else {
            byte[] bytes = str.getBytes("UTF-8");
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }
}

