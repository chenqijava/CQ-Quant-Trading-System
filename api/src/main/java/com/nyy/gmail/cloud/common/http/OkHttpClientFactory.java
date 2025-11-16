package com.nyy.gmail.cloud.common.http;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import com.nyy.gmail.cloud.entity.mongo.Socks5;

public class OkHttpClientFactory {
    public static final HttpLoggingInterceptor bodyLoggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    public static final HttpLoggingInterceptor headerloggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS);
    public static final HttpLoggingInterceptor noneloggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE);


    private static final ThreadLocal<PasswordAuthentication> localProxyConfig = new ThreadLocal<>();

    static {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return localProxyConfig.get();
            }
        });
    }

    private static OkHttpClient defaultClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(70, TimeUnit.SECONDS)
            .readTimeout(70, TimeUnit.SECONDS)
            .callTimeout(70, TimeUnit.SECONDS)
            .addNetworkInterceptor(bodyLoggingInterceptor)
            .build();

    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }

    public static OkHttpClient getDownloadClient() {
        return getDefaultClient().newBuilder()
                .readTimeout(0, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.SECONDS)
                .addNetworkInterceptor(headerloggingInterceptor)
                .build();
    }

    private static OkHttpClient socks5Client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .addNetworkInterceptor(noneloggingInterceptor)
            .build();

    public static OkHttpClient getSocks5Client(final Socks5 socks5) {
        OkHttpClient.Builder builder = socks5Client.newBuilder();
        return builder.addInterceptor(chain -> {
                    localProxyConfig.set(new PasswordAuthentication(socks5.getUsername(), socks5.getPassword().toCharArray()));
                    return chain.proceed(chain.request());
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addNetworkInterceptor(noneloggingInterceptor)
                .proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socks5.getIp(), socks5.getPort())))
                .build();
    }

    public static OkHttpClient getGoogleAi(final Socks5 socks5) {
        OkHttpClient.Builder builder = socks5Client.newBuilder();
        builder.connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
        if (socks5 != null) {
            builder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socks5.getIp(), socks5.getPort())));
            builder.addInterceptor(chain -> {
                localProxyConfig.set(new PasswordAuthentication(socks5.getUsername(), socks5.getPassword().toCharArray()));
                return chain.proceed(chain.request());
            });
        }
        return builder.build();
    }

    public static OkHttpClient getSendGrid(final Socks5 socks5) {
        OkHttpClient.Builder builder = socks5Client.newBuilder();
        builder.connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor(bodyLoggingInterceptor);
        if (socks5 != null) {
            builder.proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socks5.getIp(), socks5.getPort())));
            builder.addInterceptor(chain -> {
                localProxyConfig.set(new PasswordAuthentication(socks5.getUsername(), socks5.getPassword().toCharArray()));
                return chain.proceed(chain.request());
            });
        }
        return builder.build();
    }


    public static OkHttpClient getProxyClient(final Socks5 socks5) {
        OkHttpClient.Builder builder = socks5Client.newBuilder();
        return builder.addInterceptor(chain -> {
                    localProxyConfig.set(new PasswordAuthentication(socks5.getUsername(), socks5.getPassword().toCharArray()));
                    return chain.proceed(chain.request());
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addNetworkInterceptor(bodyLoggingInterceptor)
                .proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socks5.getIp(), socks5.getPort())))
                .build();
    }

}

