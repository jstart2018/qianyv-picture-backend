package com.jstart.qianyvpicturebackend.common.manager.uploadFile;

import cn.hutool.core.io.FileUtil;
import com.jstart.qianyvpicturebackend.exception.ResultEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void checkPictureObject(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ResultEnum.PARAMS_ERROR, "文件不能为空");
        //校验文件大小
        Long MAX_OBJECT_SIZE = 5 * 1024 * 1024L;
        ThrowUtils.throwIf(multipartFile.getSize() > MAX_OBJECT_SIZE, ResultEnum.PARAMS_ERROR, "文件不能大于5MB");
        //校验文件后缀
        //获取原文件后缀：
        String originalSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义可上传的文件后缀：
        List<String> ALLOW_OBJECT_SUFFIX = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_OBJECT_SUFFIX.contains(originalSuffix), ResultEnum.PARAMS_ERROR, "文件格式不符合要求");

    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);

    }
}
