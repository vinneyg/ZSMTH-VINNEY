package com.zfdang.zsmth_android.newsmth;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于解析HTML格式用户信息的类
 * 根据返回格式: "luckyid (岁月静好) 共上站 5556 次，发表过 4405 篇文章\n上次在  [Sat Jan 10 11:45:10 2026] 从 [39.157.26.3] 到本站一游。积分: [58897]\n离线时间[因在线上或非常断线不详] 信箱: [  ] 生命力: [888] 身份: [用户]。\n目前在站上，状态如下：\n查询网友 查询网友"
 */
public class HtmlUserInfo implements Parcelable {

    // 用户ID
    private String id;
    // 用户昵称
    private String nickname;
    // 登录次数
    private String loginCount;
    // 发帖数
    private String postCount;
    // 最后登录时间
    private String lastLoginTime;
    // 最后登录IP
    private String lastLoginIp;
    // 积分
    private String score;
    // 生命力
    private String life;
    // 身份
    private String identity;
    // 当前状态
    private String status;

    // 正则表达式模式用于解析HTML格式的用户信息
    private static final String PATTERN_ID_NICKNAME = "(\\w+)\\s*\\(([^)]+)\\)";
    private static final String PATTERN_LOGIN_COUNT = "共上站\\s*(\\d+)\\s*次";
    private static final String PATTERN_POST_COUNT = "发表过\\s*(\\d+)\\s*篇文章";
    private static final String PATTERN_LAST_LOGIN_TIME = "\\[([^\\]]+)\\]\\s*从\\s*\\[([^\\]]+)\\]";
    private static final String PATTERN_SCORE = "积分:\\s*\\[([^\\]]+)\\]";
    private static final String PATTERN_LIFE = "生命力:\\s*\\[([^\\]]+)\\]";
    private static final String PATTERN_IDENTITY = "身份:\\s*\\[([^\\]]+)\\]";
    private static final String PATTERN_STATUS = "目前在站上，状态如下：\\s*<[^>]*>?\\s*([^<\\n]+)";

    /**
     * 从HTML格式字符串解析用户信息
     * @param htmlContent HTML格式的用户信息字符串
     * @return 解析后的HtmlUserInfo对象
     */
    public static HtmlUserInfo parseFromHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return null;
        }


        HtmlUserInfo userInfo = new HtmlUserInfo();

        // 解析用户ID和昵称
        Pattern idNickPattern = Pattern.compile(PATTERN_ID_NICKNAME);
        Matcher idNickMatcher = idNickPattern.matcher(htmlContent);
        if (idNickMatcher.find()) {
            userInfo.setId(idNickMatcher.group(1)); // 用户ID
            userInfo.setNickname(idNickMatcher.group(2)); // 昵称
        }

        // 解析登录次数
        Pattern loginCountPattern = Pattern.compile(PATTERN_LOGIN_COUNT);
        Matcher loginCountMatcher = loginCountPattern.matcher(htmlContent);
        if (loginCountMatcher.find()) {
            userInfo.setLoginCount(loginCountMatcher.group(1));
        }

        // 解析发帖数
        Pattern postCountPattern = Pattern.compile(PATTERN_POST_COUNT);
        Matcher postCountMatcher = postCountPattern.matcher(htmlContent);
        if (postCountMatcher.find()) {
            userInfo.setPostCount(postCountMatcher.group(1));
        }

        // 解析最后登录时间和IP
        Pattern timeIpPattern = Pattern.compile(PATTERN_LAST_LOGIN_TIME);
        Matcher timeIpMatcher = timeIpPattern.matcher(htmlContent);
        if (timeIpMatcher.find()) {
            userInfo.setLastLoginTime(timeIpMatcher.group(1)); // 时间
            userInfo.setLastLoginIp(timeIpMatcher.group(2)); // IP
        }

        // 解析积分
        Pattern scorePattern = Pattern.compile(PATTERN_SCORE);
        Matcher scoreMatcher = scorePattern.matcher(htmlContent);
        if (scoreMatcher.find()) {
            userInfo.setScore(scoreMatcher.group(1));
        }

        // 解析生命力
        Pattern lifePattern = Pattern.compile(PATTERN_LIFE);
        Matcher lifeMatcher = lifePattern.matcher(htmlContent);
        if (lifeMatcher.find()) {
            userInfo.setLife(lifeMatcher.group(1));
        }

        // 解析身份
        Pattern identityPattern = Pattern.compile(PATTERN_IDENTITY);
        Matcher identityMatcher = identityPattern.matcher(htmlContent);
        if (identityMatcher.find()) {
            userInfo.setIdentity(identityMatcher.group(1));
        }

        // 解析状态
        Pattern statusPattern = Pattern.compile(PATTERN_STATUS);
        Matcher statusMatcher = statusPattern.matcher(htmlContent);
        if (statusMatcher.find()) {
            userInfo.setStatus(statusMatcher.group(1));
        }

        return userInfo;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(String loginCount) {
        this.loginCount = loginCount;
    }

    public String getPostCount() {
        return postCount;
    }

    public void setPostCount(String postCount) {
        this.postCount = postCount;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getLife() {
        return life;
    }

    public void setLife(String life) {
        this.life = life;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "HtmlUserInfo{" +
                "id='" + id + '\'' +
                ", nickname='" + nickname + '\'' +
                ", loginCount='" + loginCount + '\'' +
                ", postCount='" + postCount + '\'' +
                ", lastLoginTime='" + lastLoginTime + '\'' +
                ", lastLoginIp='" + lastLoginIp + '\'' +
                ", score='" + score + '\'' +
                ", life='" + life + '\'' +
                ", identity='" + identity + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    // Parcelable实现
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.nickname);
        dest.writeString(this.loginCount);
        dest.writeString(this.postCount);
        dest.writeString(this.lastLoginTime);
        dest.writeString(this.lastLoginIp);
        dest.writeString(this.score);
        dest.writeString(this.life);
        dest.writeString(this.identity);
        dest.writeString(this.status);
    }

    public HtmlUserInfo() {
        // 默认构造函数
        this.id = "";
        this.nickname = "";
        this.loginCount = "";
        this.postCount = "";
        this.lastLoginTime = "";
        this.lastLoginIp = "";
        this.score = "";
        this.life = "";
        this.identity = "";
        this.status = "";
    }

    protected HtmlUserInfo(Parcel in) {
        this.id = in.readString();
        this.nickname = in.readString();
        this.loginCount = in.readString();
        this.postCount = in.readString();
        this.lastLoginTime = in.readString();
        this.lastLoginIp = in.readString();
        this.score = in.readString();
        this.life = in.readString();
        this.identity = in.readString();
        this.status = in.readString();
    }

    public static final Creator<HtmlUserInfo> CREATOR = new Creator<HtmlUserInfo>() {
        @Override
        public HtmlUserInfo createFromParcel(Parcel source) {
            return new HtmlUserInfo(source);
        }

        @Override
        public HtmlUserInfo[] newArray(int size) {
            return new HtmlUserInfo[size];
        }
    };
}
