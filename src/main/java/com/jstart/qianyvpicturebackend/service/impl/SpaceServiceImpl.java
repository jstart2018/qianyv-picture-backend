package com.jstart.qianyvpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.enums.SpaceLevelEnum;
import com.jstart.qianyvpicturebackend.common.enums.SpaceRoleEnum;
import com.jstart.qianyvpicturebackend.common.enums.SpaceTypeEnum;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ResultEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceAddRequest;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceQueryRequest;
import com.jstart.qianyvpicturebackend.model.entity.Space;
import com.jstart.qianyvpicturebackend.model.entity.SpaceUser;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.SpaceVO;
import com.jstart.qianyvpicturebackend.model.vo.UserVO;
import com.jstart.qianyvpicturebackend.service.SpaceService;
import com.jstart.qianyvpicturebackend.mapper.SpaceMapper;
import com.jstart.qianyvpicturebackend.service.SpaceUserService;
import com.jstart.qianyvpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 28435
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-03-03 13:21:23
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    //锁对象
    private static final ConcurrentHashMap<Long, Object> userLockMap = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceUserService spaceUserService;
    //编程式事务管理器
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ResultEnum.PARAMS_ERROR);
        // 从对象中取值
        Long id = space.getId();
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 修改数据时，id 不能为空，有参数则校验
        if (!add){
            ThrowUtils.throwIf(ObjUtil.isNull(id), ResultEnum.PARAMS_ERROR, "id 不能为空");
        }
        if (Objects.equals(spaceName, "")){
            throw new BusinessException(ResultEnum.PARAMS_ERROR,"空间名称不能为空");
        }
        ThrowUtils.throwIf(spaceLevelEnum == null, ResultEnum.PARAMS_ERROR, "空间等级不能为空");
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ResultEnum.PARAMS_ERROR, "没有该空间等级");
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        //从请求实体中获取 要查询的数据，一个一个塞
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();

        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotNull(spaceType), "spaceType", spaceType);
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);

        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVO(space);
        // 关联查询用户信息
        Long userId = spaceVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            spaceVO.setUserVO(userVO);
        }
        return spaceVO;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        ThrowUtils.throwIf(space == null, ResultEnum.PARAMS_ERROR, "空间对象不能为空");
        ThrowUtils.throwIf(space.getSpaceLevel() == null, ResultEnum.PARAMS_ERROR, "空间级别错误");
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        ThrowUtils.throwIf(spaceLevelEnum == null, ResultEnum.PARAMS_ERROR, "没有该空间级别");
        //当管理员没有设置容量时才赋值
        if (space.getMaxSize() == null || space.getMaxSize() < 0) {
            space.setMaxSize(spaceLevelEnum.getMaxSize());
        }
        if (space.getMaxCount() == null || space.getMaxCount() < 0) {
            space.setMaxCount(spaceLevelEnum.getMaxCount());
        }

    }

    @Override
    public long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        //1、获取登录用户校验权限
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        if (spaceAddRequest.getSpaceLevel() != null)
            //不是管理员不能设置等级
            ThrowUtils.throwIf(!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE),
                    ResultEnum.NO_AUTH_ERROR,"没有权限设置空间等级");
        //2、校验空间请求体
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest,space);
        //填充空间信息的默认值:空间名、级别、类型
        if(StrUtil.isBlank(space.getSpaceName())){
            space.setSpaceName(loginUser.getUserName()+"的私有空间");
        }
        space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        space.setUserId(userId);
        space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        this.validSpace(space,true);
        this.fillSpaceBySpaceLevel(space);
        // 为当前用户获取一个锁对象
        Object lock = userLockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                try {
                    // 先查询数据库确认该用户是否已经有私人空间
                    if (this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType,spaceAddRequest.getSpaceType())
                            .exists()) {
                        throw new BusinessException(ResultEnum.PARAMS_ERROR, "每类空间只能创建一个");
                    }
                    // 创建私人空间的逻辑
                    boolean saveResult = this.save(space);
                    ThrowUtils.throwIf(!saveResult, ResultEnum.OPERATION_ERROR);

                    //如果插入的是团队空间，为创建者设置为管理员身份
                    if (space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())){
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        boolean saveUserSaveResult = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(saveUserSaveResult, ResultEnum.OPERATION_ERROR,"添加空间成员关系失败");
                    }
                    return space.getId();
                } finally {
                    // 清理锁对象，防止内存泄漏
                    userLockMap.remove(userId);
                }
            });
            //return newSpaceId;
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }




}




