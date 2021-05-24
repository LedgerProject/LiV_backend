package com.liv.cryptomodule.controllers;

import com.liv.cryptomodule.dto.*;
import com.liv.cryptomodule.exception.*;
import com.liv.cryptomodule.service.FileStorageService;
import com.liv.cryptomodule.util.DSM;
import com.liv.cryptomodule.util.SQLDatabaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.SQLException;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/users")
public class UserManagementController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/signup")
    public String signup(@RequestBody UserRegistrationDTO user) {
        // To validate that someone created an account just concatenate their name and email and hash it
        SignatureDTO signed = null;
        try {
            signed = DSM.sign(user.getEmail().toLowerCase(), user.getPassword());
            return SQLDatabaseConnection.createUser(user, signed.getMessageHash());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (InvalidRoleIdException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO: Redeploy the smart contract for storing event hashes
//        String did = BIM.storeEventHash(signed.getMessageHash(), signed.getPK(), signed.getSignatureValue());
        return null;
    }

    @PostMapping("/login")
    public String login(@RequestBody UserLoginDTO user) throws IOException, LoginException {
        String jwt = SQLDatabaseConnection.login(user);
        if (jwt != null) {
            return jwt;
        } else {
            throw new LoginException("Credentials invalid!");
        }
    }

    @PostMapping("/verifyJWT")
    // TODO: 03.03.2021 remove endpoint and add validation according to header auth token in each request
    public String veryfyJWT(@RequestBody JWTDTO jwt) throws JWTException {
        String decodedJwt = SQLDatabaseConnection.verifyJWT(jwt.getJwt());
        if (decodedJwt != null) {
            return decodedJwt;
        } else {
            throw new JWTException("Could not decode JWT");
        }
    }

    @RequestMapping(value = "/addKYC", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public ResponseEntity<String> addKYC(@RequestPart("email") String email, @RequestPart("first_name") String firstName,
                                         @RequestPart("last_name") String lastName, @RequestPart("second_name") String secondName,
                                         @RequestPart("address") String address, @RequestPart("nif") String nif,
                                         @RequestPart("birthday") String birthday)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        KycDTO kyc = new KycDTO(firstName, lastName, secondName, address, nif, birthday, email);
        SQLDatabaseConnection.addKYC(kyc);
        return new ResponseEntity<String>("Success", HttpStatus.OK);
    }

    @PostMapping("/storeQuestions")
    public boolean storeQuestions(@RequestBody QuestionsDTO questionsDTO) {
        try {
            SQLDatabaseConnection.storeQuestions(questionsDTO);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @GetMapping("/{userId:.+}")
    public UserModelDTO getUserData(@PathVariable String userId) throws UserNotFoundException, KycNotFoundException, IOException {
        return SQLDatabaseConnection.getUser(userId);
    }

    @PostMapping("/signup-notary-registry")
    public void signupNotaryRegistry(@RequestBody NotaryRegistryDTO user) throws IOException, InvalidRoleIdException {
        if ((Integer.parseInt(user.getRoleId()) == 1) || (Integer.parseInt(user.getRoleId()) == 2)) {
            SQLDatabaseConnection.createNotaryRegistry(user);
        } else throw new InvalidRoleIdException("Such role ID does not exist");
    }

    @PostMapping("/login-notary-registry")
    public String loginNotaryRegistry(@RequestBody NotaryRegistryLoginDTO user) throws IOException, InvalidRoleIdException {
        if ((Integer.parseInt(user.getRoleId()) == 1) || (Integer.parseInt(user.getRoleId()) == 2)) {
            return SQLDatabaseConnection.notaryRegistryLogin(user);
        } else throw new InvalidRoleIdException("Such role ID does not exist");
    }
}
