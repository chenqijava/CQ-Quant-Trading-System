package com.nyy.gmail.cloud.common;

import org.apache.commons.lang3.StringUtils;

import com.nyy.gmail.cloud.common.enums.SourceEnum;

import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Session {

  public String userID;
  public String loginUserID;
  public String session;
  public String source;

  public Session(HttpSession session) {
    this.userID = (String) session.getAttribute("userID");
    this.loginUserID = (String) session.getAttribute("LoginUserID");
    this.session = (String) session.getAttribute("session");
    this.source = (String) session.getAttribute("source");
  }

  public Session(String userID, String loginUserID, String session, String source) {
    this.userID = userID;
    this.loginUserID = loginUserID;
    this.session = session;
    this.source = source;
  }

  public boolean isLogin() {
    return StringUtils.isNotEmpty(loginUserID) && StringUtils.isNotEmpty(session);
  }

  public boolean isAdmin() {
    return "admin".equals(userID);
  }

  public boolean isApi() {
    return SourceEnum.API.name().equals(source);
  }

  public boolean isWeb() {
    return SourceEnum.WEB.name().equals(source);
  }
  
  private static final ThreadLocal<Session> localSession = new ThreadLocal<>();

  public static void setSession(Session session) {
    localSession.set(session);
  }

  public static Session currentSession() {
    return localSession.get();
  }

  public static void removeSession() {
    localSession.remove();
  }

}
