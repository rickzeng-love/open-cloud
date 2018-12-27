package com.github.lyd.base.producer.mapper;

import com.github.lyd.base.client.entity.SystemAction;
import com.github.lyd.common.mapper.CrudMapper;
import org.apache.ibatis.annotations.CacheNamespace;
import org.springframework.stereotype.Repository;

/**
 * @author liuyadu
 */
@Repository
@CacheNamespace
public interface SystemActionMapper extends CrudMapper<SystemAction> {
}
