package com.tanhua.server.utils;

import com.tanhua.server.pojo.User;

public class UserThreadLocal {
    private static final ThreadLocal<User> LOCAL = new ThreadLocal<User>();

    private UserThreadLocal() {

    }

    public static void set(User user) {
        LOCAL.set(user);
    }

    public static User get() {
        return LOCAL.get();
    }

}
