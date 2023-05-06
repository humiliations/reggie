package com.itheima.reggie.dto;

import com.itheima.reggie.entitiy.Setmeal;
import com.itheima.reggie.entitiy.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
