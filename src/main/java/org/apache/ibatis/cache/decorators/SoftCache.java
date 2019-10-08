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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;

import org.apache.ibatis.cache.Cache;

/**
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {
  // 用于保存一定数量强引用的值
  private final Deque<Object> hardLinksToAvoidGarbageCollection;
  // 引用队列，当被垃圾收集器回收时，会将软引用对象放入此队列
  private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
  private final Cache delegate;
  // 保存强引用值的数量
  private int numberOfHardLinks;

  public SoftCache(Cache delegate) {
    this.delegate = delegate;
    this.numberOfHardLinks = 256;
    this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
    this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    // 移除 被GC 回收的键值
    removeGarbageCollectedItems();
    return delegate.getSize();
  }


  public void setSize(int size) {
    this.numberOfHardLinks = size;
  }

  @Override
  public void putObject(Object key, Object value) {
    removeGarbageCollectedItems();
    // 将软件用作用到Value中  (value 中也放入 key, 为了方便删除)
    delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
  }

  @Override
  public Object getObject(Object key) {
    Object result = null;
    @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
    SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
    if (softReference != null) {
      result = softReference.get();
      if (result == null) {
        // 该值被垃圾收集器回收，移除掉该项
        delegate.removeObject(key);
      } else {
        // See #586 (and #335) modifications need more than a read lock
        // 这里以及下面的clear,为什么要加hardLinksToAvoidGarbageCollection的同步？（在WeakCache中却没有加同步）
        synchronized (hardLinksToAvoidGarbageCollection) {
          hardLinksToAvoidGarbageCollection.addFirst(result);
          if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
            // 超出容量则移除最先保存的 引用
            hardLinksToAvoidGarbageCollection.removeLast();
          }
        }
      }
    }
    return result;
  }

  @Override
  public Object removeObject(Object key) {
    // 移除 被GC 回收的键值
    removeGarbageCollectedItems();
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    synchronized (hardLinksToAvoidGarbageCollection) {
      // 这里需要清空该队列，否则即使下面调用clear，其Map清空了，但是部分值保留有引用，垃圾收集器也不会回收，会造成短暂的内存泄漏。
      hardLinksToAvoidGarbageCollection.clear();
    }
    removeGarbageCollectedItems();
    delegate.clear();
  }

  /**
   * 移除被GC 回收掉的 键值
   */
  private void removeGarbageCollectedItems() {
    SoftEntry sv;
    while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
      delegate.removeObject(sv.key);
    }
  }

  /**
   * SoftReference 会将 被GC回收掉的软引用 放入 ReferenceQueue 中
   */
  private static class SoftEntry extends SoftReference<Object> {
    // 保存与value相关联的Key，因为一旦被垃圾收集器回收，则此软引用对象会被放到关联的引用队列中，这样就可以根据Key，移除该键值对。
    private final Object key;

    SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
      super(value, garbageCollectionQueue);
      this.key = key;
    }
  }

}
