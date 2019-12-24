/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator.
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

  private final Cache delegate;

  // LinkedHashMap<Object, Object>类型对象， 有序的HashMap, 用于记录key最近使用的情况
  private Map<Object, Object> keyMap;

  // 最近最少被使用的 key(可能会被清除)
  private Object eldestKey;

  public LruCache(Cache delegate) {
    this.delegate = delegate;
    setSize(1024);
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  /**
   * 重设缓存大小时 会重置 keyMap
   * @param size
   */
  public void setSize(final int size) {
    /**
     * 初始化 keyMap, 其类型为 LinkedHashMap，
     * 覆盖其实现 removeEldestEntry():
     *   LinkedHashMap#put() 调用时会调用该方法，以决定是否在插入新的键值对后，移除老的键值对。
     * accessOrder: true:表示 LinkedHashMap 记录的顺序是 access-order
     *                   (LinkedHashMap#get()会改变其记录顺序)
     */
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;
      // 当调用 LinkedhashMap#put() 时 会调用该方法
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        /**
         * 当被装饰类的容量超出了 keyMap 的所规定的容量（由构造方法传入）后，
         * keyMap 会移除最长时间未被访问的键，并保存到 eldestKey 中，
         * 然后由 cycleKeyList 方法将 eldestKey 传给被装饰类的 removeObject 方法，移除相应的缓存项目。
         */
        boolean tooBig = size() > size;
        if (tooBig) {
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  @Override
  public void putObject(Object key, Object value) {
    delegate.putObject(key, value);
    // 检查并清除最近最少被使用的 key
    cycleKeyList(key);
  }

  @Override
  public Object getObject(Object key) {
    keyMap.get(key); //touch
    return delegate.getObject(key);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  private void cycleKeyList(Object key) {
    keyMap.put(key, key);
    if (eldestKey != null) {
      // 从装饰类中 移除相应的缓存项
      delegate.removeObject(eldestKey);
      eldestKey = null;
    }
  }

}
