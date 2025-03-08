package com.jstart.qianyvpicturebackend.controller;

import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jstart.qianyvpicturebackend.annotation.AuthCheck;
import com.jstart.qianyvpicturebackend.auth.Manager.SpaceUserAuthManager;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.entity.DeleteRequest;
import com.jstart.qianyvpicturebackend.common.entity.Result;
import com.jstart.qianyvpicturebackend.common.enums.SpaceLevelEnum;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceAddRequest;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceEditRequest;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceQueryRequest;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.jstart.qianyvpicturebackend.model.entity.Space;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.SpaceLevel;
import com.jstart.qianyvpicturebackend.model.vo.SpaceVO;
import com.jstart.qianyvpicturebackend.service.SpaceService;
import com.jstart.qianyvpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    @PostMapping("/add")
    public Result<Long> createSpace(SpaceAddRequest spaceAddRequest,HttpServletRequest request){
        ThrowUtils.throwIf(spaceAddRequest == null,ErrorEnum.PARAMS_ERROR);
        long spaceId = spaceService.createSpace(spaceAddRequest,request);
        return Result.success(spaceId);
    }


    /**
     * 删除空间
     * @param deleteRequest 删除请求参数（只有一个id）
     * @param request httpServletRequest
     * @return 返回布尔类型
     */
    @PostMapping("/delete")
    public Result<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                       HttpServletRequest request){
        ThrowUtils.throwIf(deleteRequest==null||deleteRequest.getId()==null||deleteRequest.getId() <= 0,
                ErrorEnum.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        //查询数据库数据是否存在
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space==null, ErrorEnum.NOT_FOUND_ERROR,"没有该空间");
        //判断是否有权限删除，只有本人或管理员才能删除
        System.out.println(loginUser.getId().equals(space.getUserId()));
        if(!loginUser.getId().equals(space.getUserId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
            throw new BusinessException(ErrorEnum.NO_AUTH_ERROR);
        }
        //删除数据库数据
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result,ErrorEnum.OPERATION_ERROR,"操作数据库失败");
        return Result.success(result);
    }

    /**
     * 更新空间，仅管理员可用
     * @param spaceUpdateRequest 更新空间请求
     * @param request HttpServletRequest
     * @return 返回布尔类型
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                         HttpServletRequest request){
        ThrowUtils.throwIf(spaceUpdateRequest==null,ErrorEnum.PARAMS_ERROR);
        //转移实体
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest,space);
        //校验参数合理性
        spaceService.validSpace(space,false);
        //填充空间容量
        spaceService.fillSpaceBySpaceLevel(space);
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result,ErrorEnum.SYSTEM_ERROR,"操作数据库失败");
        return Result.success(result);
    }

    /**
     * 编辑空间，用户专用
     * @param spaceEditRequest 编辑请求
     * @param request request
     * @return 布尔
     */
    @PostMapping("/edit")
    public Result<Boolean> editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request){
        if(spaceEditRequest==null||spaceEditRequest.getId() <= 0){
            throw new BusinessException(ErrorEnum.PARAMS_ERROR);
        }
        // DTO转实体
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest,space);
        //参数 如果该id的空间不存在：
        Space oldSpace = spaceService.getById(space.getId());
        ThrowUtils.throwIf(oldSpace==null,ErrorEnum.NOT_FOUND_ERROR);
        //校验权限
        User loginUser = userService.getLoginUser(request);
        if (!oldSpace.getUserId().equals(space.getUserId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)){
            throw new BusinessException(ErrorEnum.NO_AUTH_ERROR);
        }
        //校验参数合法性
        spaceService.validSpace(space,false);
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result,ErrorEnum.OPERATION_ERROR,"操作数据库失败");
        return Result.success(result);
    }

    /**
     * 根据id获取空间，未脱敏，仅管理员可用
     * @param id id
     * @return 未脱敏空间数据
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Space> getSpaceById(Long id){
        ThrowUtils.throwIf(id == null||id<=0,ErrorEnum.PARAMS_ERROR);
        //查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space!=null,ErrorEnum.NOT_FOUND_ERROR);
        return Result.success(space);
    }

    /**
     * 根据id获取空间，已脱敏，用户专用
     * @param id id
     * @return 脱敏空间数据
     */
    @PostMapping("/get/vo")
    @AuthCheck
    public Result<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request){
        ThrowUtils.throwIf(id == null||id<=0,ErrorEnum.PARAMS_ERROR);
        //查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space!=null,ErrorEnum.NOT_FOUND_ERROR);

        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space,spaceVO);

        //获取当前用户在该空间拥有的权限
        List<String> permissionList = spaceUserAuthManager
                .getPermissionList(space, userService.getLoginUser(request));
        spaceVO.setPermissionList(permissionList);


        return Result.success(spaceVO);
    }


    /**
     * 分页查询空间，管理员专用
     * @param spaceQueryRequest 分页请求dto
     * @return 分页内容
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<Space>> getSpaceList(@RequestBody SpaceQueryRequest spaceQueryRequest){
        ThrowUtils.throwIf(spaceQueryRequest==null,ErrorEnum.PARAMS_ERROR);
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();

        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return Result.success(spacePage);
    }


    /**
     * 查看空间级别列表
     * @return
     */
    @GetMapping("/list/level")
    public Result<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getDesc(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return Result.success(spaceLevelList);
    }





}
