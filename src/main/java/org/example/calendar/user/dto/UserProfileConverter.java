package org.example.calendar.user.dto;

import org.example.calendar.entity.User;
import org.springframework.core.convert.converter.Converter;

public class UserProfileConverter implements Converter<User, UserProfile> {

    @Override
    public UserProfile convert(User user) {
        return new UserProfile(user.getId(), user.getUsername());
    }
}
