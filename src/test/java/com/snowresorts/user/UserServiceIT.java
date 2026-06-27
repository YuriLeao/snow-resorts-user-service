package com.snowresorts.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snowresorts.user.domain.port.ObjectStorage;
import com.snowresorts.user.infrastructure.persistence.ProfileEntity;
import com.snowresorts.user.infrastructure.persistence.ProfileJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end test against a real Postgres (Testcontainers). The avatar object store is mocked so
 * the test does not require MinIO/S3. Authentication uses the spring-security-test {@code jwt()}
 * post-processor, so no live auth-service/JWKS is needed.
 *
 * <p>Requires a running Docker daemon; excluded from the default unit-test build via {@code -DskipITs}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UserServiceIT {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID NEW_USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String INTERNAL_SECRET = "dev-internal-secret";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("snow_resorts")
            .withUsername("snow")
            .withPassword("snow");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProfileJpaRepository profiles;
    @MockitoBean
    private ObjectStorage objectStorage;

    @BeforeEach
    void seedProfile() {
        profiles.deleteAll();
        Instant now = Instant.now();
        profiles.save(new ProfileEntity(USER_ID, "Rider", null, null, null, null,
                "friends", "friends", now, now));
    }

    @Test
    @DisplayName("GET /users/me without a token returns 401 Unauthorized")
    void getMyProfile_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/snow-resort-service/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me with a valid JWT returns the caller's profile")
    void getMyProfile_withJwt_returnsProfile() throws Exception {
        mockMvc.perform(get("/snow-resort-service/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("Rider"));
    }

    @Test
    @DisplayName("POST /users/me/avatar/upload-url returns a presigned URL and the canonical key")
    void createAvatarUploadUrl_withJwt_returnsPresignedUrl() throws Exception {
        Mockito.when(objectStorage.presignedPutUrl(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Duration.class))).thenReturn("https://s3.local/presigned");

        mockMvc.perform(post("/snow-resort-service/v1/users/me/avatar/upload-url")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://s3.local/presigned"))
                .andExpect(jsonPath("$.avatarS3Key").value("avatars/" + USER_ID + "/current.webp"));
    }

    @Test
    @DisplayName("PUT /users/me/avatar/confirm with a missing object returns 422")
    void confirmAvatar_withMissingObject_returnsUnprocessableEntity() throws Exception {
        Mockito.when(objectStorage.objectSize(Mockito.anyString())).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/snow-resort-service/v1/users/me/avatar/confirm")
                        .with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"avatarS3Key\":\"avatars/" + USER_ID + "/current.webp\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /users/internal/profiles without secret returns 401")
    void bootstrapProfile_withoutSecret_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/snow-resort-service/v1/users/internal/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","email":"new@snow-resorts.com"}"""
                                .formatted(NEW_USER_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me bootstraps a default profile when the auth account has no row yet")
    void getMyProfile_withoutProfileRow_bootstrapsOnRead() throws Exception {
        mockMvc.perform(get("/snow-resort-service/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject(NEW_USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(NEW_USER_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("User44444444"))
                .andExpect(jsonPath("$.shareStats").value("friends"));
    }

    @Test
    @DisplayName("POST /users/internal/profiles creates a default profile that GET /me can read")
    void bootstrapProfile_withValidSecret_createsProfile() throws Exception {
        mockMvc.perform(post("/snow-resort-service/v1/users/internal/profiles")
                        .header("X-Internal-Secret", INTERNAL_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","email":"new.rider@snow-resorts.com"}"""
                                .formatted(NEW_USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(NEW_USER_ID.toString()))
                .andExpect(jsonPath("$.displayName").value("new.rider"))
                .andExpect(jsonPath("$.shareStats").value("friends"));

        mockMvc.perform(get("/snow-resort-service/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.subject(NEW_USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("new.rider"));
    }
}
