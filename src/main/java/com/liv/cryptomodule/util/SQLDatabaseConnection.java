package com.liv.cryptomodule.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.liv.cryptomodule.dto.*;
import com.liv.cryptomodule.exception.InvalidRoleIdException;
import org.graalvm.compiler.word.Word;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
    private static final String secret = "fnvjksfhewjoilrh39483294032yrfsbdnz";

    private static void loadProps() throws IOException {
        InputStream input = SQLDatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties");
        if (input == null) {
            log.log(Level.SEVERE, "Can''t to find config.properties");
            return;
        }
        prop.load(input);
    }

    public static Connection connect() throws IOException, ClassNotFoundException, SQLException {
        loadProps();
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(prop.getProperty("dburl") + prop.getProperty("dbname"), prop.getProperty("dbusername"), prop.getProperty("dbpassword"));
    }

    private static Salt saltPassword(String password) {
        BigInteger salt = DSM.generateSalt();
        String saltedPassword = password + "." + salt;
        String saltedPasswordHash = DSM.SHA256hex(saltedPassword);
        return new Salt(salt, saltedPasswordHash);
    }

    public static int createWill(CreateWillDTO will) throws IOException {
        loadProps();
        String latestKycId, latestDocumentId;

        // Add KYC information in the database
        String query = "INSERT INTO " + prop.getProperty("kyctable") + " SET first_name=\"" + will.getKyc().getFirstName() + "\","
                + "middle_name=\"" + will.getKyc().getMiddleName() + "\","
                + "last_name=\"" + will.getKyc().getLastName() + "\","
                + "address=\"" + will.getKyc().getAddress() + "\","
                + "passport_number=\"" + will.getKyc().getPassportNumber() + "\";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);

        // Assign the KYC identifier to the user
        query = "SELECT kyc_id FROM " + prop.getProperty("kyctable") + " ORDER BY kyc_id DESC LIMIT 1";
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            latestKycId = resultSet.getString(1);
            query = "UPDATE " + prop.getProperty("usertable") + " SET kyc_id=" + latestKycId
                + " WHERE email=\"" + will.getEmail() + "\";";
            log.log(Level.INFO, "Executing query {0}", query);
            executeUpdateToDB(query);
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        }

        // Upload documents and add new doc to the database
        query = "INSERT INTO " + prop.getProperty("docstable") + " SET hash=\"" + DSM.SHA256hex(will.getDocument()) + "\","
                + "path=\"" + will.getDocument() + "\";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);

        // Add new request
        query = "SELECT document_id FROM " + prop.getProperty("docstable") + " ORDER BY document_id DESC LIMIT 1";
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            latestDocumentId = resultSet.getString(1);
            query = "INSERT INTO " + prop.getProperty("requeststable") + " SET user_id=" + getUserId(will.getEmail()) + ","
                    + "status_id=0, document_id=" + latestDocumentId + ";";
            log.log(Level.INFO, "Executing query {0}", query);
            executeUpdateToDB(query);
            return 0;
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        }
        return 1;
    }

    public static void rejectWill(String willId) throws IOException {

        loadProps();

        String query = "UPDATE " + prop.getProperty("requeststable") + " SET status_id=-1 WHERE request_id=" + willId + ";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);

    }

    public static void approveWill(String willId) throws IOException {
        loadProps();

        String query = "UPDATE " + prop.getProperty("requeststable") + " SET status_id=1 WHERE request_id=" + willId + ";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);

    }

    public static ArrayList<WillRequestDTO> getWillRequests() throws IOException {
        loadProps();
        ArrayList<WillRequestDTO> willRequests = new ArrayList<>();
        // Get all will requests
        String query = "SELECT * FROM " + prop.getProperty("requeststable") + ";";
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            while(resultSet.next()) {
                WillRequestDTO willRequest = getWillRequestDetails(resultSet.getString(2));
                willRequests.add(willRequest);

            }
            resultSet.close();
            return willRequests;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static WillRequestDTO getWillRequestDetails(String willRequestId) throws IOException {
        loadProps();

        String query = "SELECT * FROM " + prop.getProperty("requeststable") + " WHERE request_id=" + willRequestId + ";";
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);

            WillRequestDTO willRequest = new WillRequestDTO();
            willRequest.setRequestId(resultSet.getString(1));
            willRequest.setUserId(resultSet.getString(2));
            willRequest.setStatusId(resultSet.getString(3));
            String documentId = resultSet.getString(4);

            query = "SELECT email, did, kyc_id FROM " + prop.getProperty("usertable") + " WHERE user_id=" + resultSet.getString(2) + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            willRequest.setEmail(resultSet.getString(1));
            willRequest.setDid(resultSet.getString(2));
            String kycId = resultSet.getString(3);

            query = "SELECT * FROM kyc WHERE kyc_id=" + kycId + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            willRequest.setFullName(resultSet.getString(2) + " " + resultSet.getString(3) + " " + resultSet.getString(4));
            willRequest.setAddress(resultSet.getString(5));
            willRequest.setPassportId(resultSet.getString(6));

            query = "SELECT * FROM " + prop.getProperty("docstable") + " WHERE document_id=" + documentId + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            willRequest.setDocumentHash(resultSet.getString(2));
            willRequest.setDocumentLink(resultSet.getString(3));

            return willRequest;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new WillRequestDTO();
    }

    public static void createUser(UserRegistrationDTO user, String did) throws SQLException, IOException {

        loadProps();

        Salt salt = saltPassword(user.getPassword());

        String query = "INSERT INTO " + prop.getProperty("usertable") + " SET email=\"" + user.getEmail() + "\","
                + "password_hash=\"" + salt.getSaltedPassword() + "\","
                + "salt=\"" + salt.getSalt() + "\","
                + "did=\"" + did + "\","
                + "role_id=0;";
        log.log(Level.INFO, "Executing query {0}", query);

        executeUpdateToDB(query);
    }

    public static void createNotaryRegistry(NotaryRegistryDTO user) throws IOException {
        loadProps();
        String table;
        Salt salt = saltPassword(user.getPassword());

        if (Integer.parseInt(user.getRoleId()) == 1) {
            table = prop.getProperty("notarytable");
        } else if (Integer.parseInt(user.getRoleId()) == 2) {
            table = prop.getProperty("registrytable");
        } else {
            throw new InvalidRoleIdException("Incorrect user ID!");
        }

        String query = "INSERT INTO " + table + " SET email=\"" + user.getEmail() + "\","
                + "password_hash=\"" + salt.getSaltedPassword() + "\","
                + "salt=\"" + salt.getSalt() + "\","
                + "public_key=\"" + user.getPubKey() + "\","
                + "role_id=" + user.getRoleId();
        log.log(Level.INFO, "Executing query {0}", query);

        executeUpdateToDB(query);

    }
    private static void executeUpdateToDB(String query) {
        try {
            int result = connect().createStatement().executeUpdate(query);
            log.log(Level.INFO, String.valueOf(result));
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connect().close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static boolean isPasswordValid(UserLoginDTO user) throws IOException {

        loadProps();

//        BigInteger salt = DSM.generateSalt();
//        String saltedPassword = user.getPassword() + "." + salt;
//        String saltedPasswordHash = DSM.SHA256hex(saltedPassword);
        String query = "SELECT password_hash, salt FROM " + prop.getProperty("usertable") + " WHERE email=\"" + user.getEmail() + "\";";
        System.out.println("Executing query: " + query);
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            String dbPassword = resultSet.getString(1);
            String salt = resultSet.getString(2);
            String saltedPassword = user.getPassword() + "." + salt;
            String saltedPasswordHash = DSM.SHA256hex(saltedPassword);
            resultSet.close();
            if (dbPassword.equals(saltedPasswordHash)) {
                return true;
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean isEmailExists(UserLoginDTO user) throws IOException {

        loadProps();

        String query = "SELECT email FROM " + prop.getProperty("usertable") +
                " WHERE email=\"" + user.getEmail() + "\";";
        System.out.println("Executing query: " + query);
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            if (resultSet.next()) {
                resultSet.close();
                return true;
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void addServiceStatus(ServiceStatusDTO serviceStatus) throws IOException {

        loadProps();

        String setQuery = "INSERT INTO " + prop.getProperty("statustable") + " SET " +
                "user_id=\"" + getUserId(serviceStatus.getUserEmail()) + "\","
                + "institution=\"" + serviceStatus.getInstitution() + "\","
                + "service=\"" + serviceStatus.getService() + "\","
                + "status=\"" + serviceStatus.getStatus() + "\";";
        System.out.println("Executing query: " + setQuery);
        executeUpdateToDB(setQuery);
    }
    public static String addKYC(KycDTO kyc, String filePath) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        loadProps();
        String fileHash = DSM.SHA256hex(filePath + "." + DSM.generateSalt());
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
                + "file_hash=\"" + fileHash + "\","
                + "file_path=\"" + filePath + "\";";
        System.out.println("Executing query: " + query);
        executeUpdateToDB(query);
        return documentId;
    }
    private static String getUserPassword(String email) throws IOException {
        loadProps();
        String query = "SELECT password_hash FROM " + prop.getProperty("usertable") + " WHERE id =" +
                "\"" + getUserId(email) + "\";";
        try {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            String password = resultSet.getString(1);
            resultSet.close();
            return password;
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static ArrayList<ServiceStatusDTO> getUserServices(UserServicesDTO user) throws IOException {
        loadProps();
        ArrayList<ServiceStatusDTO> statuses = new ArrayList<>();
        String query = "SELECT * FROM " + prop.getProperty("statustable") + " WHERE " +
                "user_id=\"" + getUserId(user.getEmail()) + "\"";
        try {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            while (resultSet.next()) {
                statuses.add(new ServiceStatusDTO(resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5)));
            }
            resultSet.close();
            return statuses;
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getUserId(String userEmail) throws IOException {
        loadProps();
        String getUserIdQuery = "SELECT user_id FROM " + prop.getProperty("usertable") + " WHERE email=\""
                + userEmail + "\";";
        System.out.println("Executing query: " + getUserIdQuery);
        try {
            ResultSet resultSet = connect().createStatement().executeQuery(getUserIdQuery);
            resultSet.next();
            String userId = resultSet.getString(1);
            resultSet.close();
            return userId;
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String login(UserLoginDTO user) throws IOException {
        loadProps();
        if (isEmailExists(user) && isPasswordValid(user)) {
            String query = "SELECT user_id FROM " + prop.getProperty("usertable") + " WHERE email=\"" + user.getEmail() + "\";";
            System.out.println("Executing query: " + query);
            try {
                ResultSet resultSet = connect().createStatement().executeQuery(query);
                resultSet.next();
                String jwt = generateJWT(user, resultSet.getString(1), "0");
                resultSet.close();
                return jwt;
            }
            catch (SQLException | IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String notaryRegistryLogin(NotaryRegistryLoginDTO user) throws IOException {
        loadProps();
        String table, columnName;

        if (Integer.parseInt(user.getRoleId()) == 1) {
            table = prop.getProperty("notarytable");
            columnName = "notary_id";
        } else if (Integer.parseInt(user.getRoleId()) == 2) {
            table = prop.getProperty("registrytable");
            columnName = "registry_id";
        } else {
            throw new InvalidRoleIdException("Incorrect user ID!");
        }

        String query = "SELECT " + columnName + " FROM " + table + " WHERE email=\"" + user.getEmail() + "\";";
        log.log(Level.INFO, "Executing query {0}", query);

        try {
            ResultSet resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            UserLoginDTO login = new UserLoginDTO(user.getEmail(), user.getPassword());
            String jwt = generateJWT(login, resultSet.getString(1), user.getRoleId());
            resultSet.close();
            return jwt;
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String generateJWT(UserLoginDTO user, String ID, String accountTypeId) {
        try {
            return JWT.create()
                    .withIssuer("LiV Portal")
                    .withAudience("LiV Portal")
                    .withIssuedAt(new Date())
                    .withNotBefore(new Date())
                    .withClaim("account_type_id", accountTypeId)
                    .withClaim("user_id", ID)
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
                    .put("account_type_id", jwt.getClaim("account_type_id").asString())
                    .put("user_id", jwt.getClaim("id").asString())
                    .put("email", jwt.getClaim("email").asString());
            String JWTClaimsJSON = json.toString();
            return JWTClaimsJSON;
        } catch (JWTDecodeException e) {
            return null;
        }
    }
}