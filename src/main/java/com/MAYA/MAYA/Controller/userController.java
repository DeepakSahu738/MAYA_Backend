package com.MAYA.MAYA.Controller;

import com.MAYA.MAYA.Entity.user;
import com.MAYA.MAYA.Repository.userRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class userController {
@Autowired
private userRepository userRepository;

@GetMapping("/getAllUsers")
private ResponseEntity<List<user>> getAllUsers() {
    try{
        List<user> userList = new ArrayList<>();
        userRepository.findAll().forEach(userList::add);
        if(userList.isEmpty())
        {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        else
        {
            return new ResponseEntity<>(userList,HttpStatus.OK);
        }
    }
    catch (Exception e)
    {
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


@GetMapping("/getUserById/{id}")
private ResponseEntity<user>getUserById(@PathVariable Long id){
    try{
        Optional<user> pointeduser =userRepository.findById(id);
        if(pointeduser.isEmpty())
        {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        else {

            return new ResponseEntity<>(pointeduser.get(),HttpStatus.OK);
        }

    }
    catch (Exception e)
    {
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

@PostMapping("addUser")
    private void adduser(@RequestBody user user){


}


}
