package com.jstart.qianyvpicturebackend.common.manager.uploadFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ResultEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {
    /**
     * 校验文件格式
     * @param inputSource
     */
    @Override
    protected void checkPictureObject(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ResultEnum.PARAMS_ERROR, "url不能为空");
        //1、校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ResultEnum.PARAMS_ERROR, "url格式错误");
        }
        //2、校验协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("https://") || fileUrl.startsWith("http://")),
                ResultEnum.PARAMS_ERROR, "仅支持HTTPS和HTTP协议的地址请求");
        //3、对地址发送head请求，获取元信息
        HttpResponse resp = null;
        try {
            resp = HttpUtil.createRequest(Method.HEAD, fileUrl).timeout(5000).execute();
            //4、校验是否请求成功
            if (resp.getStatus() != HttpStatus.HTTP_OK){
                throw new BusinessException(ResultEnum.OPERATION_ERROR,"请求url错误");
            }
            //5、校验文件格式：
            String contentType = resp.header("Content-Type");
            if (!StrUtil.isBlank(contentType)){
                final List<String> list = Arrays.asList("image/jpg", "image/png", "image/jpeg", "image/webp", "image/gif");
                ThrowUtils.throwIf(!list.contains(contentType), ResultEnum.OPERATION_ERROR,"文件类型不支持："+contentType);
            }
            //6、校验文件大小
            String contentLengthStr = resp.header("Content-Length");
            if (!StrUtil.isBlank(contentLengthStr)){
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long MAX_PICTURE_SIZE = 1024 * 1024 * 5;
                    ThrowUtils.throwIf(contentLength>MAX_PICTURE_SIZE,
                            ResultEnum.OPERATION_ERROR,"图片大小不可超过5MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ResultEnum.PARAMS_ERROR,"文件大小格式错误");
                }
            }
        }catch (IORuntimeException e){
            log.error(e.getMessage(),e);
            throw new BusinessException(ResultEnum.OPERATION_ERROR,"文件请求超时，可能不允许访问");
        } finally {
            //释放资源respond
            if (resp != null) {
                resp.close();
            }
        }
        /*String fileUrl = (String) inputSource;
        // 1. 校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorEnum.PARAMS_ERROR, "文件地址为空");

        // 2. 校验 URL 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "文件地址格式不正确");
        }
        // 3. 校验 URL 的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorEnum.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址"
        );
        // 4. 发送 HEAD 请求验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl)
                    .execute();
            // 未正常返回，无需执行其他判断
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 5. 文件存在，文件类型校验
            String contentType = httpResponse.header("Content-Type");
            // 不为空，才校验是否合法，这样校验规则相对宽松
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorEnum.PARAMS_ERROR, "文件类型错误");
            }
            // 6. 文件存在，文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorEnum.PARAMS_ERROR, "文件大小不能超过 2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorEnum.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        } finally {
            // 记得释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }*/
    }

    /**
     * 获取原始文件名
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.getName(fileUrl);
    }

    /**
     * 将临时文件file填充
     * @param inputSource 输入源
     * @param file 临时文件
     * @throws IOException
     */
    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }


}
