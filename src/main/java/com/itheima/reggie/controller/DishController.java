package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entitiy.Category;
import com.itheima.reggie.entitiy.Dish;
import com.itheima.reggie.entitiy.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DIshService;
import com.itheima.reggie.service.DishFlavorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DIshService dIshService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        dIshService.saveWithFlavor(dishDto);
        // 清理某个分类下的菜品缓存
        String key = "dish_" + dishDto.getCategoryName() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null, Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        dIshService.page(pageInfo, queryWrapper);
        // 对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item)->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            // 分类的id
            Long categoryId = item.getCategoryId();
            // 根据id查出分类对象
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            dishDto.setCategoryName(categoryName);
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dIshService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        dIshService.updateWithFlavor(dishDto);
        // 清理所有dish缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        // 清理某个分类下的菜品缓存
        String key = "dish_" + dishDto.getCategoryName() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 根据条件查询对应菜品数据
     * @param dish
     * @return
     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        lambdaQueryWrapper.eq(dish.getCategoryId()!=null, Dish::getCategoryId, dish.getCategoryId());
//        lambdaQueryWrapper.eq(Dish::getStatus, 1);
//        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//
//        List<Dish> list = dIshService.list(lambdaQueryWrapper);
//        return R.success(list);
//    }

    /**
     * 根据条件查询对应菜品数据
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;
        // 动态构造redis存储的Key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        // 先尝试从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if(dishDtoList != null){
            // 如果存在，直接返回，无需查询数据库
            return R.success(dishDtoList);
        }else{
            // 如果不存在，需要查询数据库，将查询数据缓存到redis
            // 构造查询条件
            LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(dish.getCategoryId()!=null, Dish::getCategoryId, dish.getCategoryId());
            lambdaQueryWrapper.eq(Dish::getStatus, 1);
            lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

            List<Dish> list = dIshService.list(lambdaQueryWrapper);
            dishDtoList = list.stream().map((item) -> {
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item, dishDto);

                Long dishId = item.getId();
                LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(DishFlavor::getDishId, dishId);
                List<DishFlavor> dishFlavorList = dishFlavorService.list(queryWrapper);
                dishDto.setFlavors(dishFlavorList);
                return dishDto;
            }).collect(Collectors.toList());
            // 缓存到redis
            redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

            return R.success(dishDtoList);
        }
    }
}
