package com.nyy.gmail.cloud.utils.tgnet;

import java.io.File;
import java.io.IOException;

// 会话类
class Session {
    private String filePath;

    public Session(String filePath) {
        this.filePath = filePath;
    }

    // TODO 暂且不知道save有什么作用
    public void save() throws IOException {
        // 实现会话保存逻辑
    }

    // TODO 暂且不知道close有什么作用
    public void close() {
        // 实现关闭逻辑
    }
}