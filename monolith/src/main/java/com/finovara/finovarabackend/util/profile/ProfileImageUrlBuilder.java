package com.finovara.finovarabackend.util.profile;

import lombok.experimental.UtilityClass;
import java.nio.file.Paths;

@UtilityClass
public class ProfileImageUrlBuilder {

    public static String buildProfileImageUrl(String profileImagePath) {
        if (profileImagePath == null) {
            return null;
        }

        if (profileImagePath.startsWith("http://") || profileImagePath.startsWith("https://")) {
            return profileImagePath;
        }

        String filename = Paths.get(profileImagePath).getFileName().toString();

        boolean isDefault = "UserProf.png".equals(filename);

        return isDefault ? "/profile-images/default/" + filename : "/profile-images/" + filename;
    }
}
