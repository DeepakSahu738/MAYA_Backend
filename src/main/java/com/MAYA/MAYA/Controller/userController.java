package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Entity.user;
import com.MAYA.MAYA.Repository.userRepository;
import com.MAYA.MAYA.Service.genAi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class userController {
    @Autowired
    private final genAi genAi;

    @Autowired
    private userRepository userRepository;

    @Autowired
    public userController(com.MAYA.MAYA.Service.genAi genAi) {
        this.genAi = genAi;
    }

    @GetMapping("/getAllUsers")
    private ResponseEntity<List<user>> getAllUsers() {
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

    @PostMapping("/addUser")
    private ResponseEntity<user> adduser(@RequestBody user user1) {

        user user2 = userRepository.save(user1);
        return new ResponseEntity<>(user2, HttpStatus.CREATED);
    }

    @PutMapping("/updateUserById/{id}")
    private ResponseEntity<user> updateUser(@PathVariable long id, @RequestBody user newUser) {
        Optional<user> pointeduser = userRepository.findById(id);
        if (pointeduser.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {

            user updatedUser = pointeduser.get();
            updatedUser.setName(newUser.getName());
            updatedUser.setPassword(newUser.getPassword());
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


}
