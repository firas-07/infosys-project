package com.firas.server.controller;

import com.firas.server.DTO.SOSRequest;
import com.firas.server.config.TwilioConfig;
import com.firas.server.model.User;
import com.firas.server.repository.UserRepository;
import com.firas.server.model.EmergencyContact;
import com.firas.server.service.EmergencyContactService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmergencyContactService emergencyContactService;

    @Value("${twilio.fromPhoneNumber}")
    private String fromPhoneNumber;

//    @Value("${emergency.contact}")
//    private String emergencyContactPhoneNumber;  // Your emergency contact number


    @PostMapping("/send-sos")
    public ResponseEntity<String> sendSOS(@RequestBody SOSRequest sosRequest) {
        try {
            TwilioConfig.initializeTwilio();
            // Fetch all emergency contacts from the database
            List<EmergencyContact> contacts = emergencyContactService.getAllContacts();

            if (contacts.isEmpty()) {
                return ResponseEntity.status(404).body("No emergency contacts found.");
            }

            String messageBody = String.format(
                    "SOS! I am %s You are my Emergency Contact and I have an emergency at Location: Latitude: %s, Longitude: %s. Please assist immediately. " +
                            "View the location on the map: https://www.google.com/maps?q=%s,%s",
                    sosRequest.getUsername(),
                    sosRequest.getLatitude(), sosRequest.getLongitude(),
                    sosRequest.getLatitude(), sosRequest.getLongitude());
            // Loop through all emergency contacts and send an SMS to each one
            for (EmergencyContact contact : contacts) {

                // Send the message using Twilio API
                Message message = Message.creator(
                        new PhoneNumber(contact.getPhone()),  // Send SMS to contact's phone
                        new PhoneNumber(fromPhoneNumber),     // Twilio phone number
                        messageBody
                ).create();
            }

            return ResponseEntity.ok("SOS message sent to emergency contacts successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending SOS message: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User credentials, HttpServletResponse response) {
        Optional<User> user = userRepository.findByUsername(credentials.getUsername());
        if (user.isPresent() && user.get().getPassword().equals(credentials.getPassword())) {
            Cookie cookie = new Cookie("userId", user.get().getId());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of(
                    "id", user.get().getId(),
                    "username", user.get().getUsername(),
                    "role", user.get().getRole()
            ));
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

//    // Admin Dashboard
//    @GetMapping("/admin/dashboard")
//    public ResponseEntity<?> adminDashboard() {
//        return ResponseEntity.ok("Welcome to the Admin Dashboard!");
//    }

    // Professional Dashboard
    @GetMapping("/professional/dashboard")
    public ResponseEntity<?> professionalDashboard() {
        return ResponseEntity.ok("Welcome to the Professional Dashboard!");
    }
    // User Dashboard
    @GetMapping("/user/dashboard")
    public ResponseEntity<?> userDashboard() {
        return ResponseEntity.ok("Welcome to the User Dashboard!");
    }
}
