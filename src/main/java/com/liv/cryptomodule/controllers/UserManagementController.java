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
    public ResponseEntity<String> signup(@RequestBody UserRegistrationDTO user) {
        System.out.println("Received payload: " + user.toString());
        // To validate that someone created an account just concatenate their name and email and hash it
        SignatureDTO signed = null;
        try {
            signed = DSM.sign(user.getEmail().toLowerCase(), user.getPassword());
            return new ResponseEntity<>(SQLDatabaseConnection.createUser(user, signed.getMessageHash()), HttpStatus.OK);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch(UserExistsException e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
        catch (InvalidKeyException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SignatureException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return new ResponseEntity<>(throwables.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InvalidRoleIdException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //TODO: Redeploy the smart contract for storing event hashes
//        String did = BIM.storeEventHash(signed.getMessageHash(), signed.getPK(), signed.getSignatureValue());
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginDTO user) {
        String jwt = null;
        try {
            jwt = SQLDatabaseConnection.login(user);
        } catch (IOException e) {
            return new ResponseEntity<>("Could not retrieve data", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (SQLException throwables) {
            return new ResponseEntity<>("Some database queries are incorrect", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ClassNotFoundException e) {
            return new ResponseEntity<>("Database driver not found", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (jwt != null) {
            return new ResponseEntity<>(jwt, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.FORBIDDEN);
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
        System.out.println("Received payload: " + kyc.toString());
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
