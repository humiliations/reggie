package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entitiy.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机验证码短信
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        // 获取手机号
        String phone = user.getPhone();
        if(StringUtils.isNotEmpty(phone)){
            // 生成随机4位验证码
            String validateCode = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code = " + validateCode);
            // 调用短信api
            //SMSUtils.sendMessage("短信签名", "", phone, code);
            // 生成的验证码保存到session
//            session.setAttribute(phone, validateCode);
            // 生成的验证码缓存到redis中，设置有效期为5分钟
            redisTemplate.opsForValue().set(phone, validateCode, 5, TimeUnit.MINUTES);
            return R.success("手机验证码短信发送成功");
        }
        return R.error("发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        // 获取手机号
        String phone = map.get("phone").toString();
        // 获取验证码
        String code = map.get("code").toString();
        // 从session中获取保存的验证码
//        Object codeInSession = session.getAttribute(phone);
        // 从redis中获取缓存的验证码
        Object codeInSession = redisTemplate.opsForValue().get(phone);
        // 进行验证码比对
        if(codeInSession != null && codeInSession.equals(code)){
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if(user == null){
                // 新用户自动注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user", user.getId());
            // 用户登录成功，删除redis中的验证码
            redisTemplate.delete(phone);
            return R.success(user);
        }
        return R.error("登录失败");
    }
}
