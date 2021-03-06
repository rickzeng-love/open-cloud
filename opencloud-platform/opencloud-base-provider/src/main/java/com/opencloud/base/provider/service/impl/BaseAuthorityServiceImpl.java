package com.opencloud.base.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opencloud.base.client.constants.BaseConstants;
import com.opencloud.base.client.constants.ResourceType;
import com.opencloud.base.client.model.AccessAuthority;
import com.opencloud.base.client.model.BaseApiAuthority;
import com.opencloud.base.client.model.BaseMenuAuthority;
import com.opencloud.base.client.model.entity.*;
import com.opencloud.base.provider.mapper.*;
import com.opencloud.base.provider.service.*;
import com.opencloud.common.constants.CommonConstants;
import com.opencloud.common.exception.OpenAlertException;
import com.opencloud.common.exception.OpenException;
import com.opencloud.common.mybatis.base.service.impl.BaseServiceImpl;
import com.opencloud.common.security.Authority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * 系统权限管理
 * 对菜单、操作、API等进行权限分配操作
 *
 * @author liuyadu
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseAuthorityServiceImpl  extends BaseServiceImpl<BaseAuthorityMapper, BaseAuthority> implements BaseAuthorityService {

    @Autowired
    private BaseAuthorityMapper baseAuthorityMapper;
    @Autowired
    private BaseRoleAuthorityMapper baseRoleAuthorityMapper;
    @Autowired
    private BaseUserAuthorityMapper baseUserAuthorityMapper;
    @Autowired
    private BaseAppAuthorityMapper baseAppAuthorityMapper;
    @Autowired
    private BaseResourceMenuService baseResourceMenuService;
    @Autowired
    private BaseResourceOperationService baseResourceOperationService;
    @Autowired
    private BaseResourceApiService baseResourceApiService;
    @Autowired
    private BaseRoleService baseRoleService;
    @Autowired
    private BaseUserService baseUserService;
    @Autowired
    private BaseAppService baseAppService;

    private final static String SEPARATOR = "_";

    @Value("${spring.application.name}")
    private String DEFAULT_SERVICE_ID;

    /**
     * 获取访问权限列表
     *
     * @return
     */
    @Override
    public List<AccessAuthority> findAccessAuthority() {
        return baseAuthorityMapper.selectAccessAuthority();
    }

    /**
     * 获取菜单权限列表
     *
     * @return
     */
    @Override
    public List<BaseMenuAuthority> findMenuAuthority(Integer status) {
        Map map = Maps.newHashMap();
        map.put("status", status);
        List<BaseMenuAuthority> authorities = baseAuthorityMapper.selectMenuAuthority(map);
        return authorities;

    }

    @Override
    public List<BaseApiAuthority> findApiAuthority(Integer isOpen, String serviceId) {
        Map map = Maps.newHashMap();
        map.put("serviceId", serviceId);
        map.put("status", 1);
        map.put("isOpen", isOpen);
        List<BaseApiAuthority> authorities = baseAuthorityMapper.selectApiAuthority(map);
        return authorities;

    }


    /**
     * 保存或修改权限
     *
     * @param resourceId
     * @param resourceType
     * @return 权限Id
     */
    @Override
    public BaseAuthority saveOrUpdateAuthority(Long resourceId, ResourceType resourceType) {
        BaseAuthority baseAuthority = getAuthority(resourceId, resourceType);
        String authority = null;
        if (baseAuthority == null) {
            baseAuthority = new BaseAuthority();
        }
        if (ResourceType.menu.equals(resourceType)) {
            BaseResourceMenu menu = baseResourceMenuService.getMenu(resourceId);
            authority = ResourceType.menu.name().toUpperCase() + SEPARATOR + menu.getMenuCode();
            baseAuthority.setMenuId(resourceId);
            baseAuthority.setStatus(menu.getStatus());
        }
        if (ResourceType.action.equals(resourceType)) {
            BaseResourceOperation operation = baseResourceOperationService.getOperation(resourceId);
            authority = ResourceType.action.name().toUpperCase() + SEPARATOR + operation.getOperationCode();
            baseAuthority.setOperationId(resourceId);
            baseAuthority.setStatus(operation.getStatus());
        }
        if (ResourceType.api.equals(resourceType)) {
            BaseResourceApi api = baseResourceApiService.getApi(resourceId);
            authority = api.getApiCode();
            baseAuthority.setApiId(resourceId);
            baseAuthority.setStatus(api.getStatus());
        }
        if (authority == null) {
            return null;
        }
        // 设置权限标识
        baseAuthority.setAuthority(authority);
        if (baseAuthority.getAuthorityId() == null) {
            // 新增权限
            baseAuthorityMapper.insert(baseAuthority);
        } else {
            // 修改权限
            baseAuthorityMapper.updateById(baseAuthority);
        }
        return baseAuthority;
    }

    /**
     * 移除权限
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    @Override
    public void removeAuthority(Long resourceId, ResourceType resourceType) {
        if (isGranted(resourceId, resourceType)) {
            throw new OpenAlertException(String.format("资源已被授权,不允许删除!取消授权后,再次尝试!"));
        }
        QueryWrapper<BaseAuthority> queryWrapper = buildQueryWrapper(resourceId, resourceType);
        baseAuthorityMapper.delete(queryWrapper);
    }

    /**
     * 获取权限
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    @Override
    public BaseAuthority getAuthority(Long resourceId, ResourceType resourceType) {
        if (resourceId == null || resourceType == null) {
            return null;
        }
        QueryWrapper<BaseAuthority> queryWrapper = buildQueryWrapper(resourceId, resourceType);
        return baseAuthorityMapper.selectOne(queryWrapper);
    }

    /**
     * 是否已被授权
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    @Override
    public Boolean isGranted(Long resourceId, ResourceType resourceType) {
        BaseAuthority authority = getAuthority(resourceId, resourceType);
        if (authority == null || authority.getAuthorityId() == null) {
            return false;
        }
        QueryWrapper<BaseRoleAuthority> roleQueryWrapper = new QueryWrapper();
        roleQueryWrapper.lambda().eq(BaseRoleAuthority::getAuthorityId, authority.getAuthorityId());
        int roleGrantedCount = baseRoleAuthorityMapper.selectCount(roleQueryWrapper);
        QueryWrapper<BaseUserAuthority> userQueryWrapper = new QueryWrapper();
        userQueryWrapper.lambda().eq(BaseUserAuthority::getAuthorityId, authority.getAuthorityId());
        int userGrantedCount = baseUserAuthorityMapper.selectCount(userQueryWrapper);
        QueryWrapper<BaseAppAuthority> appQueryWrapper = new QueryWrapper();
        appQueryWrapper.lambda().eq(BaseAppAuthority::getAuthorityId, authority.getAuthorityId());
        int appGrantedCount = baseAppAuthorityMapper.selectCount(appQueryWrapper);
        return roleGrantedCount > 0 || userGrantedCount > 0 || appGrantedCount > 0;
    }

    /**
     * 构建权限对象
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    private QueryWrapper<BaseAuthority> buildQueryWrapper(Long resourceId, ResourceType resourceType) {
        QueryWrapper<BaseAuthority> queryWrapper = new QueryWrapper();
        if (ResourceType.menu.equals(resourceType)) {
            queryWrapper.lambda().eq(BaseAuthority::getMenuId, resourceId);
        }
        if (ResourceType.action.equals(resourceType)) {
            queryWrapper.lambda().eq(BaseAuthority::getOperationId, resourceId);
        }
        if (ResourceType.api.equals(resourceType)) {
            queryWrapper.lambda().eq(BaseAuthority::getApiId, resourceId);
        }
        return queryWrapper;
    }


    /**
     * 移除应用权限
     *
     * @param appId
     */
    @Override
    public void removeAppAuthority(String appId) {
        QueryWrapper<BaseAppAuthority> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(BaseAppAuthority::getAppId, appId);
        baseAppAuthorityMapper.delete(queryWrapper);
    }


    /**
     * 角色授权
     *
     * @param roleId       角色ID
     * @param expireTime   过期时间,null表示长期,不限制
     * @param authorityIds 权限集合
     * @return
     */
    @Override
    public void addRoleAuthority(Long roleId, Date expireTime, String... authorityIds) {
        if (roleId == null) {
            return;
        }
        // 清空角色已有授权
        QueryWrapper<BaseRoleAuthority> roleQueryWrapper = new QueryWrapper();
        roleQueryWrapper.lambda().eq(BaseRoleAuthority::getRoleId, roleId);
        baseRoleAuthorityMapper.delete(roleQueryWrapper);
        BaseRoleAuthority authority = null;
        if (authorityIds != null && authorityIds.length > 0) {
            for (String id : authorityIds) {
                authority = new BaseRoleAuthority();
                authority.setAuthorityId(Long.parseLong(id));
                authority.setRoleId(roleId);
                authority.setExpireTime(expireTime);
                // 批量添加授权
                baseRoleAuthorityMapper.insert(authority);
            }

        }
    }

    /**
     * 用户授权
     *
     * @param userId       用户ID
     * @param expireTime   过期时间,null表示长期,不限制
     * @param authorityIds 权限集合
     * @return
     */
    @Override
    public void addUserAuthority(Long userId, Date expireTime, String... authorityIds) {
        if (userId == null) {
            return;
        }
        BaseUser user = baseUserService.getUserById(userId);
        if (user == null) {
            return;
        }
        if (CommonConstants.ROOT.equals(user.getUserName())) {
            throw new OpenAlertException("默认用户无需授权!");
        }
        // 获取用户角色列表
        List<Long> roleIds = baseRoleService.getUserRoleIds(userId);
        // 清空用户已有授权
        // 清空角色已有授权
        QueryWrapper<BaseUserAuthority> userQueryWrapper = new QueryWrapper();
        userQueryWrapper.lambda().eq(BaseUserAuthority::getUserId, userId);
        baseUserAuthorityMapper.delete(userQueryWrapper);
        BaseUserAuthority authority = null;
        if (authorityIds != null && authorityIds.length > 0) {
            for (String id : authorityIds) {
                if (roleIds != null && roleIds.size() > 0) {
                    // 防止重复授权
                    if (isGrantByRoles(id, roleIds.toArray(new Long[roleIds.size()]))) {
                        continue;
                    }
                }
                authority = new BaseUserAuthority();
                authority.setAuthorityId(Long.parseLong(id));
                authority.setUserId(userId);
                authority.setExpireTime(expireTime);
                baseUserAuthorityMapper.insert(authority);
            }
        }
    }

    /**
     * 应用授权
     *
     * @param appId        应用ID
     * @param expireTime   过期时间,null表示长期,不限制
     * @param authorityIds 权限集合
     * @return
     */
    @CacheEvict(value = {"apps"}, key = "'client:'+#appId")
    @Override
    public void addAppAuthority(String appId, Date expireTime, String... authorityIds) {
        if (appId == null) {
            return;
        }
        BaseApp baseApp = baseAppService.getAppInfo(appId);
        if (baseApp == null) {
            return;
        }
        if (baseApp.getIsPersist().equals(BaseConstants.ENABLED)) {
            throw new OpenAlertException(String.format("保留数据,不允许授权"));
        }
        // 清空应用已有授权
        QueryWrapper<BaseAppAuthority> appQueryWrapper = new QueryWrapper();
        appQueryWrapper.lambda().eq(BaseAppAuthority::getAppId, appId);
        baseAppAuthorityMapper.delete(appQueryWrapper);
        BaseAppAuthority authority = null;
        if (authorityIds != null && authorityIds.length > 0) {
            for (String id : authorityIds) {
                authority = new BaseAppAuthority();
                authority.setAuthorityId(Long.parseLong(id));
                authority.setAppId(appId);
                authority.setExpireTime(expireTime);
                baseAppAuthorityMapper.insert(authority);
            }
        }
    }

    /**
     * 应用授权-添加单个权限
     *
     * @param appId
     * @param expireTime
     * @param authorityId
     */
    @CacheEvict(value = {"apps"}, key = "'client:'+#appId")
    @Override
    public void addAppAuthority(String appId, Date expireTime, String authorityId) {
        BaseAppAuthority appAuthority = new BaseAppAuthority();
        appAuthority.setAppId(appId);
        appAuthority.setAuthorityId(Long.parseLong(authorityId));
        appAuthority.setExpireTime(expireTime);
        QueryWrapper<BaseAppAuthority> appQueryWrapper = new QueryWrapper();
        appQueryWrapper.lambda()
                .eq(BaseAppAuthority::getAppId, appId)
                .eq(BaseAppAuthority::getAuthorityId, authorityId);
        int count = baseAppAuthorityMapper.selectCount(appQueryWrapper);
        if (count > 0) {
            return;
        }
        baseAppAuthorityMapper.insert(appAuthority);
    }

    /**
     * 获取应用已授权权限
     *
     * @param appId
     * @return
     */
    @Override
    public List<Authority> findAppGrantedAuthority(String appId) {
        return baseAppAuthorityMapper.selectAppGrantedAuthority(appId);
    }

    /**
     * 获取角色已授权权限
     *
     * @param roleId
     * @return
     */
    @Override
    public List<Authority> findRoleGrantedAuthority(Long roleId) {
        return baseRoleAuthorityMapper.selectRoleGrantedAuthority(roleId);
    }

    /**
     * 获取所有可用权限
     *
     * @param type = null 查询全部  type = 1 获取菜单和操作 type = 2 获取API
     * @return
     */
    @Override
    public List<Authority> findGrantedAuthority(String type) {
        Map map = Maps.newHashMap();
        map.put("type", type);
        map.put("status", 1);
        return baseAuthorityMapper.selectAllGrantedAuthority(map);
    }

    /**
     * 获取用户已授权权限
     *
     * @param userId
     * @param root   超级管理员
     * @return
     */
    @Override
    public List<Authority> findUserGrantedAuthority(Long userId, Boolean root) {
        if (root) {
            // 超级管理员返回所有
            return findGrantedAuthority("1");
        }
        List<Authority> authorities = Lists.newArrayList();
        List<BaseRole> rolesList = baseRoleService.getUserRoles(userId);
        if (rolesList != null) {
            for (BaseRole role : rolesList) {
                // 加入角色已授权
                List<Authority> roleGrantedAuthority = findRoleGrantedAuthority(role.getRoleId());
                if (roleGrantedAuthority != null && roleGrantedAuthority.size() > 0) {
                    authorities.addAll(roleGrantedAuthority);
                }
            }
        }
        // 加入用户特殊授权
        List<Authority> userGrantedAuthority = baseUserAuthorityMapper.selectUserGrantedAuthority(userId);
        if (userGrantedAuthority != null && userGrantedAuthority.size() > 0) {
            authorities.addAll(userGrantedAuthority);
        }
        // 权限去重
        HashSet h = new HashSet(authorities);
        authorities.clear();
        authorities.addAll(h);
        return authorities;
    }

    /**
     * 获取用户已授权权限详情
     *
     * @param userId
     * @param root   超级管理员
     * @return
     */
    @Override
    public List<BaseMenuAuthority> findUserMenuAuthority(Long userId, Boolean root) {
        if (root) {
            // 超级管理员返回所有
            return findMenuAuthority(null);
        }
        // 用户权限列表
        List<BaseMenuAuthority> authorities = Lists.newArrayList();
        List<BaseRole> rolesList = baseRoleService.getUserRoles(userId);
        if (rolesList != null) {
            for (BaseRole role : rolesList) {
                // 加入角色已授权
                List<BaseMenuAuthority> roleGrantedAuthority = baseRoleAuthorityMapper.selectRoleMenuAuthority(role.getRoleId());
                if (roleGrantedAuthority != null && roleGrantedAuthority.size() > 0) {
                    authorities.addAll(roleGrantedAuthority);
                }
            }
        }
        // 加入用户特殊授权
        List<BaseMenuAuthority> userGrantedAuthority = baseUserAuthorityMapper.selectUserMenuAuthority(userId);
        if (userGrantedAuthority != null && userGrantedAuthority.size() > 0) {
            authorities.addAll(userGrantedAuthority);
        }
        // 权限去重
        HashSet h = new HashSet(authorities);
        authorities.clear();
        authorities.addAll(h);
        return authorities;
    }

    /**
     * 检测权限是否被多个角色授权
     *
     * @param authorityId
     * @param roleIds
     * @return
     */
    @Override
    public Boolean isGrantByRoles(String authorityId, Long... roleIds) {
        if (roleIds == null || roleIds.length == 0) {
            throw new OpenException("roleIds is empty");
        }
        QueryWrapper<BaseRoleAuthority> roleQueryWrapper = new QueryWrapper();
        roleQueryWrapper.lambda()
                .in(BaseRoleAuthority::getRoleId, roleIds)
                .eq(BaseRoleAuthority::getAuthorityId, authorityId);
        int count = baseRoleAuthorityMapper.selectCount(roleQueryWrapper);
        return count > 0;
    }


}
