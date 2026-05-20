package com.finovara.finovarabackend.util.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileImageUrlBuilderTest {

    @Nested
    class NullPathTests {
        private String profileImagePath;

        @BeforeEach
        void setUp() {
            profileImagePath = null;
        }

        @Test
        void shouldReturnNullWhenProfileImagePathIsNull() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(profileImagePath);

            assertThat(result).isNull();
        }
    }

    @Nested
    class ExternalUrlTests {
        private String httpPath;
        private String httpsPath;

        @BeforeEach
        void setUp() {
            httpPath = "http://example.com/image.png";
            httpsPath = "https://example.com/image.png";
        }

        @Test
        void shouldReturnUnchangedHttpUrl() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(httpPath);

            assertThat(result).isEqualTo(httpPath);
        }

        @Test
        void shouldReturnUnchangedHttpsUrl() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(httpsPath);

            assertThat(result).isEqualTo(httpsPath);
        }
    }

    @Nested
    class DefaultProfileImageTests {

        private String defaultImagePath;
        private String defaultImageFilename;

        @BeforeEach
        void setUp() {
            defaultImagePath = "/uploads/default/UserProf.png";
            defaultImageFilename = "UserProf.png";
        }

        @Test
        void shouldBuildDefaultProfileImageUrlFromPath() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(defaultImagePath);

            assertThat(result)
                    .isEqualTo("/profile-images/default/UserProf.png");
        }

        @Test
        void shouldBuildDefaultProfileImageUrlFromFilename() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(defaultImageFilename);

            assertThat(result)
                    .isEqualTo("/profile-images/default/UserProf.png");
        }
    }

    @Nested
    class CustomProfileImageTests {

        private String unixPath;
        private String windowsPath;
        private String filename;

        @BeforeEach
        void setUp() {
            unixPath = "/uploads/profile/avatar123.png";
            windowsPath = "C:\\images\\profile\\avatar.png";
            filename = "avatar.png";
        }

        @Test
        void shouldBuildRegularProfileImageUrlFromUnixPath() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(unixPath);

            assertThat(result)
                    .isEqualTo("/profile-images/avatar123.png");
        }

        @Test
        void shouldBuildRegularProfileImageUrlFromWindowsPath() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(windowsPath);

            assertThat(result)
                    .isEqualTo("/profile-images/avatar.png");
        }

        @Test
        void shouldBuildRegularProfileImageUrlFromFilename() {
            String result = ProfileImageUrlBuilder.buildProfileImageUrl(filename);

            assertThat(result)
                    .isEqualTo("/profile-images/avatar.png");
        }
    }
}