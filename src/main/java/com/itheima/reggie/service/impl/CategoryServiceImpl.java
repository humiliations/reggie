package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entitiy.Category;
import com.itheima.reggie.entitiy.Dish;
import com.itheima.reggie.entitiy.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DIshService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DIshService dIshService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除之前进行关联判断
     * @param id
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        int count = dIshService.count(dishLambdaQueryWrapper);

        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);
        int count1 = setmealService.count(setmealLambdaQueryWrapper);

        // 是否关联了菜品
        if(count > 0){
            throw new CustomException("当前分类下关联了菜品不能删除");
        }
        // 是否关联了套餐
        if(count1 > 0){
            throw new CustomException("当前分类下关联了套餐不能删除");
        }

        // 正常删除分类
        super.removeById(id);
    }

}
