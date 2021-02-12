package com.liv.cryptomodule.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liv.cryptomodule.dto.*;
import com.liv.cryptomodule.exception.InvalidRoleIdException;
import com.liv.cryptomodule.exception.WrongPageOrderException;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLDatabaseConnection {

    private static Properties prop = new Properties();
    private static final Logger log = java.util.logging.Logger.getLogger(SQLDatabaseConnection.class.getName());
    private static final String secret = "fnvjksfhewjoilrh39483294032yrfsbdnz";

    private static final String IPFS_URL = "http://18.192.22.193:8080/ipfs/";
    private static final String IPFS_BASE_URL = "http://18.192.22.193:5001";
    private static final String ADD_FILE_ENDPOINT = "/api/v0/add";

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

    public static int createWill(CreateWillDTO will, MultipartFile file) throws IOException {
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
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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

        saveDocumentToIpfs(file);
        // Add new request
        query = "SELECT document_id FROM " + prop.getProperty("docstable") + " ORDER BY document_id DESC LIMIT 1";
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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

    private static void saveDocumentToIpfs(MultipartFile file) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        okhttp3.RequestBody body =
                okhttp3.RequestBody.create(okhttp3.MediaType.parse(file.getContentType()), file.getBytes());

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)  // Header to show we are sending a Multipart Form Data
                .addFormDataPart("file", file.getOriginalFilename(), body) // file param
                .build();

        System.out.println(IPFS_BASE_URL + ADD_FILE_ENDPOINT);

        Request request = new Request.Builder()
                .url(IPFS_BASE_URL + ADD_FILE_ENDPOINT)
                .post(multipartBody)
                .build();
        Response response = client.newCall(request).execute();


        String resp = response.body().string();

        System.out.println(resp);
        ObjectMapper objectMapper = new ObjectMapper();
        IpsfResponse ipsfResponse = objectMapper.readValue(resp, IpsfResponse.class);

        addDocumentToDb(ipsfResponse.getHash());
    }

    private static void addDocumentToDb(String ipfsDocumentHash) throws IOException {
        loadProps();

        String query = "INSERT INTO " + prop.getProperty("docstable") + " SET hash=\"" + ipfsDocumentHash + "\","
                + "path=\"" + IPFS_URL + ipfsDocumentHash + "\";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);
    }

    public static void rejectWill(List<WillRequestIdDTO> willIdList) throws IOException {

        loadProps();

        for(WillRequestIdDTO willId: willIdList) {
            String query = "UPDATE " + prop.getProperty("requeststable") + " SET status_id=-1 WHERE request_id=" + willId.getWillRequestId() + ";";
            log.log(Level.INFO, "Executing query {0}", query);
            executeUpdateToDB(query);
        }
    }

    public static void approveWill(List<WillRequestIdDTO> willIdList) throws IOException {
        loadProps();
        for (WillRequestIdDTO willId : willIdList) {
            String query = "UPDATE " + prop.getProperty("requeststable") + " SET status_id=1 WHERE request_id=" + willId.getWillRequestId() + ";";
            log.log(Level.INFO, "Executing query {0}", query);
            executeUpdateToDB(query);
        }
    }

    public static ArrayList<WillRequestDTO> getWillRequests(PageAndFilterDTO pageAndFilterDTO) throws IOException, WrongPageOrderException {
        loadProps();
        ArrayList<WillRequestDTO> willRequests = new ArrayList<>();
        // Get all will requests
        String query = "SELECT * FROM " + prop.getProperty("requeststable") + ";";
        if (pageAndFilterDTO != null) {
            if (pageAndFilterDTO.getFilterDto() != null) {
                FilterDTO filterDTO = pageAndFilterDTO.getFilterDto();
                StringBuilder sb = new StringBuilder(query);
                sb.deleteCharAt(query.length() - 1);
                if (!filterDTO.getAccountId().isEmpty() && filterDTO.getAccountId().chars().allMatch(Character::isDigit)) {
                    sb.append(" WHERE user_id = ").append(filterDTO.getAccountId());
                }

                if (!filterDTO.getRecipientId().isEmpty() && filterDTO.getRecipientId().chars().allMatch(Character::isDigit)) {
                    if (sb.toString().contains("WHERE")) {
                        sb.append(" AND ").append("recipient_id = ").append(filterDTO.getRecipientId());
                    } else {
                        sb.append(" WHERE recipient_id = ").append(filterDTO.getRecipientId());
                    }
                }

                if (!filterDTO.getStatus().isEmpty() && filterDTO.getStatus().chars().allMatch(Character::isDigit)) {
                    if (sb.toString().contains("WHERE")) {
                        sb.append(" AND ").append("status_id = ").append(filterDTO.getStatus());
                    } else {
                        sb.append(" WHERE status_id = ").append(filterDTO.getStatus());
                    }
                }

                sb.append(";");
                query = sb.toString();
            }

            if (pageAndFilterDTO.getPageDto() != null) {
                if ((pageAndFilterDTO.getPageDto().getOrder().equalsIgnoreCase("DESC") || pageAndFilterDTO.getPageDto().getOrder().equalsIgnoreCase("ASC")) && pageAndFilterDTO.getPageDto().getLimit() >= 1) {
                    StringBuilder sb = new StringBuilder(query);
                    sb.deleteCharAt(query.length() - 1);
                    sb.append(" ORDER BY request_id ").append(pageAndFilterDTO.getPageDto().getOrder().toUpperCase());
                    sb.append(" LIMIT ").append(pageAndFilterDTO.getPageDto().getLimit()).append(";");
                    query = sb.toString();
                } else {
                    throw new WrongPageOrderException("Wrong page params");
                }
            }

            System.out.println(query);


        }

        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            while (resultSet.next()) {
                if (resultSet.getString(2) != null) {
                    WillRequestDTO willRequest = getWillRequestDetails(resultSet.getString(2));
                    willRequests.add(willRequest);
                }
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

    public static WillRequestDTO getWillRequestDetails(@NotNull String willRequestId) throws IOException {
        loadProps();

        String query = "SELECT * FROM " + prop.getProperty("requeststable") + " WHERE request_id=" + willRequestId + ";";
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();

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

    public static void createUser(UserRegistrationDTO user, String did) throws SQLException, IOException, InvalidRoleIdException {
        String table;

        loadProps();

        Salt salt = saltPassword(user.getPassword());

        switch (Integer.parseInt(user.getRoleId())) {
            case 0:
                table = prop.getProperty("usertable");
                break;
            case 1:
                table = prop.getProperty("notarytable");
                break;
            case 2:
                table = prop.getProperty("registrytable");
                break;
            default:
                throw new InvalidRoleIdException("Such role does not exist!");
        }

        String query = "INSERT INTO " + table + " SET email=\"" + user.getEmail() + "\","
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
        } else {
            table = prop.getProperty("registrytable");
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
        try (Connection connection = connect()) {
            int result = connection.createStatement().executeUpdate(query);
            log.log(Level.INFO, String.valueOf(result));
        } catch (SQLException | IOException | ClassNotFoundException e) {
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
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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
        try (Connection connection = connect()) {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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
        try (Connection connection = connect()) {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(getUserIdQuery);
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
            try (Connection connection = connect()) {
                ResultSet resultSet = connection.createStatement().executeQuery(query);
                resultSet.next();
                String jwt = generateJWT(user, resultSet.getString(1), "0");
                resultSet.close();
                return jwt;
            } catch (SQLException | IOException | ClassNotFoundException e) {
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
        } else {
            table = prop.getProperty("registrytable");
            columnName = "registry_id";
        }

        String query = "SELECT " + columnName + " FROM " + table + " WHERE email=\"" + user.getEmail() + "\";";
        log.log(Level.INFO, "Executing query {0}", query);

        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
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
                    .put("user_id", jwt.getClaim("user_id").asString())
                    .put("email", jwt.getClaim("email").asString());
            String JWTClaimsJSON = json.toString();
            return JWTClaimsJSON;
        } catch (JWTDecodeException e) {
            return null;
        }
    }
}