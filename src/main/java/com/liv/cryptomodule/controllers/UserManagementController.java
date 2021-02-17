package com.liv.cryptomodule.controllers;

import com.liv.cryptomodule.dto.*;
import com.liv.cryptomodule.exception.*;
import com.liv.cryptomodule.payload.UploadFileResponse;
import com.liv.cryptomodule.service.FileStorageService;
import com.liv.cryptomodule.util.DSM;
import com.liv.cryptomodule.util.SQLDatabaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    public void signup(@RequestBody UserRegistrationDTO user)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, SQLException, InvalidRoleIdException {
        // To validate that someone created an account just concatenate their name and email and hash it
        SignatureDTO signed = DSM.sign(user.getEmail().toLowerCase(), user.getPassword());
//TODO: Redeploy the smart contract for storing event hashes
//        String did = BIM.storeEventHash(signed.getMessageHash(), signed.getPK(), signed.getSignatureValue());
        SQLDatabaseConnection.createUser(user, signed.getMessageHash());
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
    public String veryfyJWT(@RequestBody JWTDTO jwt) throws JWTException {
        String decodedJwt = SQLDatabaseConnection.verifyJWT(jwt.getJwt());
        if (decodedJwt != null) {
            return decodedJwt;
        } else {
            throw new JWTException("Could not decode JWT");
        }
    }

    @RequestMapping(value = "/addKYC", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public UploadFileResponse addKYC(@RequestPart("firstName") String firstName, @RequestPart("middleName") String middleName,
                                     @RequestPart("lastName") String lastName, @RequestPart("passportID") String passportId, @RequestPart("email") String email,
                                     @RequestPart("file") MultipartFile file)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        KycDTO kyc = new KycDTO(firstName, middleName, lastName, passportId, email);
        String fileName = fileStorageService.storeFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();
        SQLDatabaseConnection.addKYC(kyc, fileDownloadUri);
        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
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
