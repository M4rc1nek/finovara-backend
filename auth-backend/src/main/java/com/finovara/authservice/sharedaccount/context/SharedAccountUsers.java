package com.finovara.authservice.util.user.context;

import com.finovara.authservice.user.model.User;

public record SharedAccountUsers (
        User owner,
        User member
){
}
