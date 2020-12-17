package com.liv.cryptomodule.controllers;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;

import com.liv.cryptomodule.util.BIM;
import com.liv.cryptomodule.util.DSM;
import com.liv.cryptomodule.util.SQLDatabaseConnection;
import com.liv.cryptomodule.dto.*;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/users")
public class UserManagementController {
    @PostMapping("/signup")
    public void signup(@RequestBody UserRegistrationDTO user)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, SQLException {
        // To validate that someone created an account just concatenate their name and email and hash it
        SignatureDTO signed = DSM.sign(user.getFirstName().toLowerCase() + user.getLastName().toLowerCase() + user.getEmail().toLowerCase(), user.getPassword());
        String did = BIM.storeEventHash(signed.getMessageHash(), signed.getPK(), signed.getSignatureValue());
        SQLDatabaseConnection.createUser(user, signed.getMessageHash());
    }
    @PostMapping("/login")
    public String login(@RequestBody UserLoginDTO user) {
        return SQLDatabaseConnection.login(user);
    }
    @PostMapping("/verifyJWT")
    public String veryfyJWT(@RequestBody JWTDTO jwt) {
        return SQLDatabaseConnection.verifyJWT(jwt.getJwt());
    }
    @PostMapping("/addKYC")
    public String addKYC(@RequestBody KycDTO kyc) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return SQLDatabaseConnection.addKYC(kyc);
    }
}
