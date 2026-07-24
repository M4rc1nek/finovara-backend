package com.finovara.authservice.sharedaccount.context;

import com.finovara.authservice.user.model.User;

public record SharedAccountUsers (
        User owner,
        User member
){
    public User getParticualUser(Long userId) {
        return owner.getId().equals(userId) ? member : owner;
    }
}
