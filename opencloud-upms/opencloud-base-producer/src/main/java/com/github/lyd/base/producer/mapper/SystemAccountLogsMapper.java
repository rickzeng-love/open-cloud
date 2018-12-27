package com.github.lyd.base.producer.mapper;


import com.github.lyd.common.mapper.CrudMapper;
import com.github.lyd.base.client.entity.SystemAccountLogs;
import org.apache.ibatis.annotations.CacheNamespace;
import org.springframework.stereotype.Repository;

/**
 * @author liuyadu
 */
@Repository
@CacheNamespace
public interface SystemAccountLogsMapper extends CrudMapper<SystemAccountLogs> {
}
