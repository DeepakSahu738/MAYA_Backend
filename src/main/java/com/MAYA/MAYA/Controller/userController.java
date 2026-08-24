package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.userDTO.loginUser;
import com.MAYA.MAYA.Entity.UserSocialAccount;
import com.MAYA.MAYA.Entity.user;
import com.MAYA.MAYA.Repository.UserSocialAccountRepository;
import com.MAYA.MAYA.Repository.WeeklyGoalRepository;
import com.MAYA.MAYA.Repository.instagram.*;
import com.MAYA.MAYA.Repository.userRepository;
import com.MAYA.MAYA.Service.DemoLangChainServiceImpl;
import com.MAYA.MAYA.Service.OtpService;
import com.MAYA.MAYA.Service.genAi;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceInstagram;
import com.MAYA.MAYA.Service.phyllo.PhylloService;
import com.MAYA.MAYA.Security.jwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;
// use this to compare existing password to new password - PasswordEncoder.matches(rawPassword, encodedPassword)

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:5173","https://mayamanage.com","https://mayamanage-84da8.firebaseapp.com",
        "https://mayamanage-84da8.web.app"})
public class userController {
    @Autowired
    private final genAi genAi;

    @Autowired
    private final DemoLangChainServiceImpl langChainService;

    @Autowired
    private final LangChainAiServiceInstagram langChainServiceNew;

    @Autowired
    private userRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserSocialAccountRepository socialAccountRepository;

    @Autowired
    private PhylloService phylloService;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ScheduledPostRepository scheduledPostRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private HashtagPerformanceRepository hashtagPerformanceRepository;

    @Autowired
    private TopCommenterRepository topCommenterRepository;

    @Autowired
    private WeeklyGoalRepository weeklyGoalRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    public userController(com.MAYA.MAYA.Service.genAi genAi, DemoLangChainServiceImpl langChainService, LangChainAiServiceInstagram langChainServiceNew) {
        this.genAi = genAi;

        this.langChainService = langChainService;

        this.langChainServiceNew = langChainServiceNew;
    }

    @GetMapping("/getAllUsers")
    private ResponseEntity<List<user>> getAllUsers() {
        System.out.println("yaha aaya toh tha ");
        try {
            List<user> userList = new ArrayList<>();
            userRepository.findAll().forEach(userList::add);
            if (userList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(userList, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/getUserById/{id}")
    private ResponseEntity<user> getUserById(@PathVariable Long id) {
        try {
            Optional<user> pointeduser = userRepository.findById(id);
            if (pointeduser.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {

                return new ResponseEntity<>(pointeduser.get(), HttpStatus.OK);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/registerUser")
    private ResponseEntity<?> adduser(@RequestBody user user1) {
        // Legacy endpoint — redirect to OTP flow
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of("error", "Direct registration disabled. Use /auth/send-otp instead."));
    }

    /**
     * POST /auth/send-otp
     * Body: { "email": "...", "name": "...", "firstname": "...", "lastname": "...", "password": "..." }
     * Generates OTP, stores pending registration, sends verification email.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest request) {
        // Validate email format
        if (request.email() == null || !request.email().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }

        // Validate required fields
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        }
        if (request.password() == null || request.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        if (request.firstname() == null || request.firstname().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "First name is required"));
        }
        if (request.lastname() == null || request.lastname().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Last name is required"));
        }

        // Check if email already registered
        Optional<user> existingUser = userRepository.findByEmailIgnoreCase(request.email());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Email already registered. Try logging in."));
        }

        // Generate and send OTP
        String result = otpService.generateAndSendOtp(
            request.email(), request.name(), request.firstname(), request.lastname(), request.password()
        );

        return switch (result) {
            case "SENT" -> ResponseEntity.ok(Map.of("message", "OTP sent to your email", "email", request.email()));
            case "RATE_LIMITED" -> ResponseEntity.status(429)
                .body(Map.of("error", "Too many attempts. Please wait 10 minutes."));
            case "EMAIL_FAILED" -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send OTP. Please try again."));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong. Please try again."));
        };
    }

    /**
     * POST /auth/verify-otp
     * Body: { "email": "...", "otp": "123456" }
     * Verifies OTP → creates user → returns JWT (auto-login).
     */
    @PostMapping("/verify-otp")
    @Transactional
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        if (request.email() == null || request.otp() == null || request.otp().length() != 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and 6-digit OTP are required"));
        }

        OtpService.VerifyResult result = otpService.verifyOtp(request.email(), request.otp());

        return switch (result.status()) {
            case SUCCESS -> {
                // Create the user from stored OTP data
                com.MAYA.MAYA.Entity.OtpVerification record = result.getRecord();

                // Double-check email not taken (race condition safety)
                Optional<user> existing = userRepository.findByEmailIgnoreCase(request.email());
                if (existing.isPresent()) {
                    yield ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email already registered."));
                }

                user newUser = new user();
                newUser.setEmail(record.getEmail().toLowerCase());
                newUser.setName(record.getName());
                newUser.setFirstname(record.getFirstname());
                newUser.setLastname(record.getLastname());
                newUser.setPassword(record.getPasswordHash()); // already hashed
                newUser.setRole(com.MAYA.MAYA.Entity.Role.USER);
                user savedUser = userRepository.save(newUser);

                // Generate JWT directly (auto-login after registration)
                String token = jwtTokenProvider.generateTokenForUser(savedUser);

                yield ResponseEntity.ok(Map.of(
                    "message", "Account created successfully",
                    "token", token,
                    "userId", savedUser.getUserId()
                ));
            }
            case INVALID -> {
                Integer remaining = result.getRemainingAttempts();
                yield ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid OTP",
                    "attemptsRemaining", remaining != null ? remaining : 0
                ));
            }
            case EXPIRED -> ResponseEntity.status(410)
                .body(Map.of("error", "OTP expired. Please request a new one."));
            case MAX_ATTEMPTS -> ResponseEntity.status(429)
                .body(Map.of("error", "Too many failed attempts. Request a new OTP."));
            case NOT_FOUND -> ResponseEntity.status(404)
                .body(Map.of("error", "No pending verification found. Start registration again."));
        };
    }

    record SendOtpRequest(String email, String name, String firstname, String lastname, String password) {}
    record VerifyOtpRequest(String email, String otp) {}
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody loginUser loginUser) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginUser.getEmail(), loginUser.getPassword()));
            System.out.println("Authentication: " + authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtTokenProvider.generateToken(authentication);
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Authentication failed");
        }
    }


    @PutMapping("/updateUserById/{id}")
    private ResponseEntity<user> updateUser(@PathVariable long id, @RequestBody user newUser) {
        Optional<user> pointeduser = userRepository.findById(id);
        if (pointeduser.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {

            user updatedUser = pointeduser.get();
            updatedUser.setName(newUser.getName());
            updatedUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
            updatedUser.setEmail((newUser.getEmail()));
            user user2 = userRepository.save(updatedUser);
            return new ResponseEntity<>(user2, HttpStatus.OK);
        }

    }

    @DeleteMapping("/deleteUserById/{id}")
    @Transactional
    private ResponseEntity<?> deleteUser(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        // Verify the JWT user matches the requested deletion
        Long jwtUserId = extractUserId(jwt);
        if (!jwtUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own account");
        }

        Optional<user> pointeduser = userRepository.findById(id);
        if (pointeduser.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Find all social accounts owned by this user
        List<UserSocialAccount> socialAccounts = socialAccountRepository.findByUserId(id);

        int creatorsDeleted = 0;
        int postsDeleted = 0;
        int commentsDeleted = 0;

        for (UserSocialAccount account : socialAccounts) {
            // Disconnect from Phyllo
            if (account.getPhylloAccountId() != null) {
                phylloService.disconnectAccount(account.getPhylloAccountId());
            }

            // Delete all creator data
            if (account.getCreator() != null) {
                Long creatorId = account.getCreator().getId();

                postsDeleted += postRepository.findByCreatorIdOrderByPostedAtDesc(creatorId).size();
                commentsDeleted += commentRepository.findByCreatorIdOrderByCommentedAtDesc(creatorId).size();

                // Delete children first (FK constraints)
                commentRepository.deleteByCreatorId(creatorId);
                scheduledPostRepository.deleteByCreatorId(creatorId);
                weeklyReportRepository.deleteByCreatorId(creatorId);
                hashtagPerformanceRepository.deleteByCreatorId(creatorId);
                topCommenterRepository.deleteByCreatorId(creatorId);
                postRepository.deleteByCreatorId(creatorId);

                // Delete creator
                creatorRepository.deleteById(creatorId);
                creatorsDeleted++;
            }
        }

        // Delete all social account links
        socialAccountRepository.deleteAll(socialAccounts);

        // Delete weekly goals
        weeklyGoalRepository.deleteByUserId(id);

        // Finally delete the user
        userRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
            "message", "Account and all associated data permanently deleted",
            "deletedData", Map.of(
                "creators", creatorsDeleted,
                "posts", postsDeleted,
                "comments", commentsDeleted,
                "socialAccounts", socialAccounts.size()
            )
        ));
    }

    @GetMapping("/genAiResponse")
    private Mono<String> getAiResponse()
    {
        return genAi.getGenAiResponse();
    }

    @GetMapping("/langChatResponse")
    private String getLangChatResponse(@RequestParam(value = "message", defaultValue = "What time is it now?") String prompt)
    {
        return langChainServiceNew.chat(prompt);

    }

    private Long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userID");
        if (claim instanceof Long) return (Long) claim;
        if (claim instanceof Integer) return ((Integer) claim).longValue();
        if (claim instanceof Number) return ((Number) claim).longValue();
        throw new RuntimeException("Invalid userID claim in JWT");
    }

}
