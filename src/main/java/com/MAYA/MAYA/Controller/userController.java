package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.DTO.userDTO.loginUser;
import com.MAYA.MAYA.Entity.user;
import com.MAYA.MAYA.Repository.userRepository;
import com.MAYA.MAYA.Service.DemoLangChainServiceImpl;
import com.MAYA.MAYA.Service.genAi;
import com.MAYA.MAYA.Service.contentServices.LangChainAiServiceInstagram;
import com.MAYA.MAYA.Security.jwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// use this to compare existing password to new password - PasswordEncoder.matches(rawPassword, encodedPassword)

@RestController
@RequestMapping("/auth")
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
    private ResponseEntity<user> adduser(@RequestBody user user1) {

        String hashedPassword = passwordEncoder.encode(user1.getPassword());
        user1.setPassword(hashedPassword);
        user user2 = userRepository.save(user1);
        // use this to compare existing password to new password - PasswordEncoder.matches(rawPassword, encodedPassword)

        return new ResponseEntity<>(user2, HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody loginUser loginUser) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginUser.getName(), loginUser.getPassword()));
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
    private ResponseEntity<user> deleteUser(@PathVariable Long id) {
        Optional<user> pointeduser = userRepository.findById(id);
        if (pointeduser.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {

            userRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        }

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



}
