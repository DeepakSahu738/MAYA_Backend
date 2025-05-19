package com.MAYA.MAYA.Controller;


import com.MAYA.MAYA.Entity.contactMessage;
import com.MAYA.MAYA.Repository.contactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/contact")
@CrossOrigin(origins = "http://localhost:5173")
public class contactMessageController {

    @Autowired
    private contactMessageRepository contactMessageRepository;

    @GetMapping("/getAllContactMessages")
    private ResponseEntity<List<contactMessage>> getAllContactMessages() {
        try {
            List<contactMessage> contactMessageList = new ArrayList<>();
            contactMessageRepository.findAll().forEach(contactMessageList::add);
            if (contactMessageList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(contactMessageList, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/getContactMessagesById/{id}")
    private ResponseEntity<contactMessage> getUserById(@PathVariable Long id) {
        try {
            Optional<contactMessage> pointedMessages = contactMessageRepository.findById(id);
            if (pointedMessages.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {

                return new ResponseEntity<>(pointedMessages.get(), HttpStatus.OK);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/addContactMessages")
    private ResponseEntity<contactMessage> addContactMessages(@RequestBody contactMessage contactMessage1) {

        try {
            // Basic validation (optional but recommended)
            if (contactMessage1.getEmail() == null || contactMessage1.getEmail().isEmpty()
            ||  contactMessage1.getName() ==null || contactMessage1.getName().isEmpty()
            ||  contactMessage1.getMessage() == null || contactMessage1.getMessage().isEmpty()){
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            contactMessage contactMessage2 = contactMessageRepository.save(contactMessage1);
            return new ResponseEntity<>(contactMessage2, HttpStatus.CREATED);

        } catch (Exception e) {
            // Log the error for debugging (in real apps, use a logger)
            contactMessage contactMessage3 = new contactMessage();
            contactMessage3.setMessage("Failed to register user");
            e.printStackTrace();
            return new ResponseEntity<>(contactMessage3,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/updateContactMessageById/{id}")
    private ResponseEntity<contactMessage> updateContactMessageById(@PathVariable long id, @RequestBody contactMessage newContactMessage) {
        Optional<contactMessage> pointedMessage = contactMessageRepository.findById(id);
        if (pointedMessage.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {

            contactMessage updatedContactMessage = pointedMessage.get();
            updatedContactMessage.setName(newContactMessage.getName());
            updatedContactMessage.setMessage(newContactMessage.getMessage());
            updatedContactMessage.setEmail((newContactMessage.getEmail()));
            contactMessage contactMessage2 = contactMessageRepository.save(updatedContactMessage);
            return new ResponseEntity<>(updatedContactMessage, HttpStatus.OK);
        }

    }

    @DeleteMapping("/deleteContactMessageById/{id}")
    private ResponseEntity<contactMessage> deleteContactMessage(@PathVariable Long id) {
        Optional<contactMessage> pointeduser = contactMessageRepository.findById(id);
        if (pointeduser.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {

            contactMessageRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        }

    }




}
