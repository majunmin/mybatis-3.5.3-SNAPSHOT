package org.apache.ibatis.learn.learn2;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.util.Map;

@Intercepts({
  @Signature(type = Map.class, method = "get", args = {Object.class})})
public class AlwaysMapPlugin implements Interceptor {
  @Override
  public Object intercept(Invocation invocation) {
    return "Always";
  }

}
