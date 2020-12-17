package com.liv.cryptomodule.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.liv.cryptomodule.dto.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLDatabaseConnection {

    private static Properties prop = new Properties();
    private static final Logger log = java.util.logging.Logger.getLogger(SQLDatabaseConnection.class.getName());
    private static String connectionURL;
    private static Statement statement;
    private static final String secret = "fnvjksfhewjoilrh39483294032yrfsbdnz";

    private static void loadProps() throws IOException {
        InputStream input = SQLDatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            log.log(Level.SEVERE, "Can''t to find config.properties");
            return;
        }
        prop.load(input);
    }

    public static void initialize() throws IOException {
        loadProps();
        try {
            System.out.println("Getting ready to connect...");
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(prop.getProperty("dburl") + prop.getProperty("dbname"), prop.getProperty("dbusername"), prop.getProperty("dbpassword"));
            System.out.println("Database connection initialized!");
            statement = connection.createStatement();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void createUser(UserRegistrationDTO user, String did) throws SQLException {

        BigInteger salt = DSM.generateSalt();
        String saltedPassword = user.getPassword() + "." + salt;
        String saltedPasswordHash = DSM.SHA256hex(saltedPassword);

        String query = "INSERT INTO " + prop.getProperty("usertable") + " SET firstname=\""+ user.getFirstName() + "\","
                + "lastname=\"" + user.getLastName() + "\","
                + "email=\"" + user.getEmail() + "\","
                + "password=\"" + saltedPasswordHash + "\","
                + "salt=\"" + salt + "\","
                + "did=\"" + did + "\";";
        System.out.println("Executing query: " + query);
        try {
            int result = statement.executeUpdate(query);
            System.out.println(result);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static boolean isPasswordValid(UserLoginDTO user) {

//        BigInteger salt = DSM.generateSalt();
//        String saltedPassword = user.getPassword() + "." + salt;
//        String saltedPasswordHash = DSM.SHA256hex(saltedPassword);
        String query = "SELECT password, salt FROM " + prop.getProperty("usertable") + " WHERE email=\"" + user.getEmail() + "\";";
        System.out.println("Executing query: " + query);
        try {
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next();
            String dbPassword = resultSet.getString(1);
            String salt = resultSet.getString(2);
            String saltedPassword = user.getPassword() + "." + salt;
            String saltedPasswordHash = DSM.SHA256hex(saltedPassword);
            if (dbPassword.equals(saltedPasswordHash)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean isEmailExists(UserLoginDTO user) {

        String query = "SELECT email FROM " + prop.getProperty("usertable") +
                " WHERE email=\"" + user.getEmail() + "\";";
        System.out.println("Executing query: " + query);
        try {
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void addServiceStatus(ServiceStatusDTO serviceStatus) {

        String setQuery = "INSERT INTO " + prop.getProperty("statustable") + " SET " +
                "user_id=\"" + getUserId(serviceStatus.getUserEmail()) + "\","
                + "institution=\"" + serviceStatus.getInstitution() + "\","
                + "service=\"" + serviceStatus.getService() + "\","
                + "status=\"" + serviceStatus.getStatus() + "\";";
        try {
            System.out.println("Executing query: " + setQuery);
            statement.executeUpdate(setQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static String addKYC(KycDTO kyc) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        String fileHash = DSM.SHA256hex(kyc.getFile() + "." + DSM.generateSalt());
        String userPassword = getUserPassword(kyc.getEmail());
        SignatureDTO signature = DSM.sign(fileHash, userPassword);
        System.out.println(BIM.storeEventHash(fileHash, signature.getPK(), signature.getSignatureValue()));
        String documentId = BIM.mintDocumentToken();
        String query = "INSERT INTO " + prop.getProperty("kyctable") + " SET " +
                "user_id=\"" + getUserId(kyc.getEmail()) + "\","
                + "first_name=\"" + kyc.getFirstName() + "\","
                + "middle_name=\"" + kyc.getMiddleName() + "\","
                + "last_name=\"" + kyc.getLastName() + "\","
                + "passport_id=\"" + kyc.getPassportID() + "\","
                + "email=\"" + kyc.getEmail() + "\","
                + "document_id=\"" + documentId + "\","
                + "file_hash=\"" + fileHash + "\";";
        try {
            System.out.println("Executing query: " + query);
            statement.executeUpdate(query);
            return documentId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String getUserPassword(String email) {
        String query = "SELECT password FROM " + prop.getProperty("usertable") + " WHERE id =" +
                "\"" + getUserId(email) + "\";";
        try {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next();
            return resultSet.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static ArrayList<ServiceStatusDTO> getUserServices(UserServicesDTO user) {
        ArrayList<ServiceStatusDTO> statuses = new ArrayList<>();
        String query = "SELECT * FROM " + prop.getProperty("statustable") + " WHERE " +
                "user_id=\"" + getUserId(user.getEmail()) + "\"";
        try {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                statuses.add(new ServiceStatusDTO(resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5)));
            }
            return statuses;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String getUserId(String userEmail) {
        String getUserIdQuery = "SELECT id FROM " + prop.getProperty("usertable") + " WHERE email=\""
                + userEmail + "\";";
        System.out.println("Executing query: " + getUserIdQuery);
        try {
            ResultSet resultSet = statement.executeQuery(getUserIdQuery);
            resultSet.next();
            return resultSet.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String login(UserLoginDTO user) {
        if (isEmailExists(user) && isPasswordValid(user)) {
            String query = "SELECT id, firstname, lastname FROM " + prop.getProperty("usertable") + " WHERE email=\"" + user.getEmail() + "\";";
            System.out.println("Executing query: " + query);
            try {
                ResultSet resultSet = statement.executeQuery(query);
                resultSet.next();
                return generateJWT(user, resultSet.getString(1), resultSet.getString(2), resultSet.getString(3));
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    private static String generateJWT(UserLoginDTO user, String ID, String firstName, String lastName) {
        try {
            return JWT.create()
                    .withIssuer("LiV Portal")
                    .withAudience("LiV Portal")
                    .withIssuedAt(new Date())
                    .withNotBefore(new Date())
                    .withClaim("id", ID)
                    .withClaim("firstname", firstName)
                    .withClaim("lastname", lastName)
                    .withClaim("email", user.getEmail())
                    .sign(Algorithm.HMAC256(secret));
        } catch (JWTCreationException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String verifyJWT(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("LiV Portal")
                    .build();
            DecodedJWT jwt = verifier.verify(token);
             JSONObject json = new JSONObject();
             try {
                 Field changeMap = json.getClass().getDeclaredField("map");
                 changeMap.setAccessible(true);
                 changeMap.set(json, new LinkedHashMap<>());
                 changeMap.setAccessible(false);
             } catch (IllegalAccessException | NoSuchFieldException e) {
                 e.printStackTrace();
             }
             json
                    .put("id", jwt.getClaim("id").asString())
                    .put("firstname", jwt.getClaim("firstname").asString())
                    .put("lastname", jwt.getClaim("lastname").asString())
                    .put("email", jwt.getClaim("email").asString());
             String JWTClaimsJSON = json.toString();
             return JWTClaimsJSON;
        } catch (JWTDecodeException e) {
            return null;
        }
    }
}
