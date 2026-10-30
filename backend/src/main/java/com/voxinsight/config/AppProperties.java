package com.voxinsight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private long demoUserId = 1;
  private String voskModel = "models/vosk-model-small-cn-0.22";
  private String dashscopeApiKey = "";
  private String dashscopeModel = "qwen-plus";
  private String dashscopeBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

  public long getDemoUserId() {
    return demoUserId;
  }

  public void setDemoUserId(long demoUserId) {
    this.demoUserId = demoUserId;
  }

  public String getVoskModel() {
    return voskModel;
  }

  public void setVoskModel(String voskModel) {
    this.voskModel = voskModel;
  }

  public String getDashscopeApiKey() {
    return dashscopeApiKey;
  }

  public void setDashscopeApiKey(String dashscopeApiKey) {
    this.dashscopeApiKey = dashscopeApiKey;
  }

  public String getDashscopeModel() {
    return dashscopeModel;
  }

  public void setDashscopeModel(String dashscopeModel) {
    this.dashscopeModel = dashscopeModel;
  }

  public String getDashscopeBaseUrl() {
    return dashscopeBaseUrl;
  }

  public void setDashscopeBaseUrl(String dashscopeBaseUrl) {
    this.dashscopeBaseUrl = dashscopeBaseUrl;
  }
}
