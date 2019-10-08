package org.apache.ibatis.learn.learn2;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.learn.model.AuthorDO;

public interface AuthorDao {
    AuthorDO findOne(@Param("id") int id);
}
