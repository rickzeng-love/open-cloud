package com.opencloud.base.provider.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.opencloud.base.client.model.entity.BaseResourceOperation;
import com.opencloud.base.client.model.entity.BaseResourceOperationApi;
import com.opencloud.base.client.model.entity.BaseRole;
import com.opencloud.common.model.PageParams;
import com.opencloud.common.mybatis.base.service.IBaseService;

import java.util.List;

/**
 * 操作资源管理
 * @author liuyadu
 */
public interface BaseResourceOperationService extends IBaseService<BaseResourceOperation> {
    /**
     * 分页查询
     *
     * @param pageParams
     * @return
     */
    IPage<BaseResourceOperation> findListPage(PageParams pageParams);

    /**
     * 根据主键获取操作
     *
     * @param operationId
     * @return
     */
    BaseResourceOperation getOperation(Long operationId);

    /**
     * 查询菜单下所有操作
     * @param menuId
     * @return
     */
    List<BaseResourceOperation> findListByMenuId(Long menuId);

    /**
     * 检查操作编码是否存在
     *
     * @param operationCode
     * @return
     */
    Boolean isExist(String operationCode);


    /**
     * 添加操作资源
     *
     * @param operation
     * @return
     */
    BaseResourceOperation addOperation(BaseResourceOperation operation);

    /**
     * 修改操作资源
     *
     * @param operation
     * @return
     */
    BaseResourceOperation updateOperation(BaseResourceOperation operation);

    /**
     * 移除操作
     *
     * @param operationId
     * @return
     */
    void removeOperation(Long operationId);

    /**
     * 操作绑定接口资源
     * @param operationId
     * @param apiIds
     * @return
     */
    void addOperationApi(Long operationId,String ... apiIds);

    /**
     * 查询操作已绑定接口
     * @param operationId
     * @return
     */
    List<BaseResourceOperationApi> findOperationApi(Long operationId);

    /**
     * 移除操作已绑定接口
     * @param operationId
     */
    void  removeOperationApi(Long operationId);
}
