package com.jstart.qianyvpicturebackend.webSocket.strategy;


import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ResultEnum;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditMessageTypeEnum;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
public class PictureActionContext implements InitializingBean {

    @Resource
    private List<PictureEditActionStrategy> editActionStrategies;



    private Map<PictureEditMessageTypeEnum,PictureEditActionStrategy> strategyMap;


    //返回对应类型的的策略类
    public PictureEditActionStrategy getStrategy(PictureEditMessageTypeEnum messageTypeEnum) {
        PictureEditActionStrategy strategy = strategyMap.get(messageTypeEnum);
        if (strategy == null) {
            throw new BusinessException(ResultEnum.SYSTEM_ERROR,"该策略初始化异常");
        }
        return strategy;
    }



    //初始化map
    @Override
    public void afterPropertiesSet() throws Exception {
        for (PictureEditActionStrategy editActionStrategy : editActionStrategies) {
            strategyMap.put(editActionStrategy.getPictureEditActionEnum(), editActionStrategy);
        }
    }

}
