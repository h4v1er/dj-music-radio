package org.example.mall.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.mall.entity.MallOrder;
import org.example.mall.mapper.MallOrderMapper;
import org.example.mall.service.MallOrderService;
import org.springframework.stereotype.Service;

@Service
public class MallOrderServiceImpl extends ServiceImpl<MallOrderMapper, MallOrder> implements MallOrderService {
}
