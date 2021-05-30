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
import com.liv.cryptomodule.exception.KycNotFoundException;
import com.liv.cryptomodule.exception.UserNotFoundException;
import com.liv.cryptomodule.exception.WrongPageOrderException;
import com.liv.cryptomodule.payload.EmailPayload;
import com.liv.cryptomodule.service.EmailService;
import com.liv.cryptomodule.service.MailContentBuilder;
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
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: Remove unused methods and maybe split into several classes
public class SQLDatabaseConnection {

    private static Properties prop = new Properties();
    private static final Logger log = java.util.logging.Logger.getLogger(SQLDatabaseConnection.class.getName());
    private static final String secret = "fnvjksfhewjoilrh39483294032yrfsbdnz";

    private static final String IPFS_URL = "http://18.192.22.193:8080/ipfs/";
    private static final String IPFS_BASE_URL = "http://18.192.22.193:5001";
    private static final String ADD_FILE_ENDPOINT = "/api/v0/add";

    private static final String USER_TABLE = "usertable";
    private static final String NOTARY_TABLE = "notarytable";
    private static final String REGISTRY_TABLE = "registrytable";
    private static final String KYC_TABLE = "kyctable";
    private static final String DOCS_TABLE = "docstable";
    private static final String REQUESTS_TABLE = "requeststable";
    private static final String STATUSES_TABLE = "statustable";

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

    //TODO: Add check of whether sender_id is in the database
    //TODO: Refactor recipient_id check
    public static int createWill(String senderId, String recipientEmail, MultipartFile file) throws IOException, UserNotFoundException, SQLException, ClassNotFoundException {
        loadProps();
        //  Update requests tbl query
        String requestsUpdate = "UPDATE " + prop.getProperty(REQUESTS_TABLE) + " SET status_id = -3 WHERE user_id = " + senderId + ";";

        String latestKycId, latestDocumentId;

        String senderQuery = "SELECT * FROM " + prop.getProperty(USER_TABLE) + " WHERE user_id= " + senderId + ";";
        String recipientQuery = "SELECT * FROM " + prop.getProperty(USER_TABLE) + " WHERE email= " + "\"" + recipientEmail + "\";";

        try (Connection connection = connect()) {
            System.out.println("Executing query: " + senderQuery);
            ResultSet resultSet = connection.createStatement().executeQuery(senderQuery);
            if (resultSet.next()) {
                resultSet.close();
            } else {
                throw new UserNotFoundException("No user was found for this userId -> " + senderId);
            }
            resultSet = connection.createStatement().executeQuery(recipientQuery);
            if (resultSet.next()) {
                resultSet.close();
            } else {
                log.info("No user was found for this email -> " + recipientEmail + "; Creating.");
                String draftUserInsertQuery = "INSERT INTO " + prop.getProperty(USER_TABLE) + " (email) VALUES ('" + recipientEmail + "');";
                resultSet = connection.createStatement().executeQuery(draftUserInsertQuery);
                resultSet.close();
                // throw new UserNotFoundException("No user was found for this email -> " + recipientEmail);
            }
        } catch (SQLException | ClassNotFoundException | UserNotFoundException e) {
            e.printStackTrace();
            if (e instanceof UserNotFoundException) {
                throw e;
            }
        }

        saveDocumentToIpfs(file);

        String query = "SELECT document_id FROM " + prop.getProperty(DOCS_TABLE) + " ORDER BY document_id DESC LIMIT 1";
        try (Connection connection = connect()) {
            //  upd requests tbl exec; 
            log.info("Executing query: " + requestsUpdate);
            connection.createStatement().executeUpdate(requestsUpdate);

            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();
            latestDocumentId = resultSet.getString(1);

            String recipientId = SQLDatabaseConnection.getUserId(recipientEmail);

            query = "INSERT INTO " + prop.getProperty(REQUESTS_TABLE) + " SET user_id=" + senderId + ","
                    + "status_id=0, document_id=" + latestDocumentId
                    + ", recipient_id=" + recipientId + ";";
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

        String query = "INSERT INTO " + prop.getProperty(DOCS_TABLE) + " SET hash=\"" + ipfsDocumentHash + "\","
                + "path=\"" + IPFS_URL + ipfsDocumentHash + "\";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);
    }

    public static void rejectWill(List<WillRequestIdDTO> willIdList) throws IOException {
        loadProps();

        for (WillRequestIdDTO willId : willIdList) {
            String query = "UPDATE " + prop.getProperty(REQUESTS_TABLE) + " SET status_id=-1 WHERE request_id=" + willId.getWillRequestId() + ";";
            log.log(Level.INFO, "Executing query {0}", query);
            executeUpdateToDB(query);
        }
    }

    public static void approveWill(List<WillRequestIdDTO> willIdList) throws IOException {
        loadProps();

        for (WillRequestIdDTO willId : willIdList) {
            String query = "UPDATE " + prop.getProperty(REQUESTS_TABLE) + " SET status_id=1 WHERE request_id=" + willId.getWillRequestId() + ";";
            log.log(Level.INFO, "Executing query {0}", query);
            executeUpdateToDB(query);
        }
    }

    public static ArrayList<WillRequestResponseDTO> getWillRequests(PageAndFilterDTO pageAndFilterDTO) throws IOException, WrongPageOrderException {
        loadProps();

        ArrayList<WillRequestResponseDTO> willRequests = new ArrayList<>();
        // Get all will requests
        String query = "SELECT * FROM " + prop.getProperty(REQUESTS_TABLE) + ";";
        if (pageAndFilterDTO != null) {
            if (pageAndFilterDTO.getFilterDto() != null) {
                FilterDTO filterDTO = pageAndFilterDTO.getFilterDto();
                StringBuilder sb = new StringBuilder(query);
                sb.deleteCharAt(query.length() - 1);
                if (filterDTO.getAccountId() != null && !filterDTO.getAccountId().isEmpty() && filterDTO.getAccountId().matches("-?\\d+")) {
                    sb.append(" WHERE user_id = ").append(filterDTO.getAccountId());
                }

                if (filterDTO.getRecipientId() != null && !filterDTO.getRecipientId().isEmpty() && filterDTO.getRecipientId().matches("-?\\d+")) {
                    if (sb.toString().contains("WHERE")) {
                        sb.append(" AND ").append("recipient_id = ").append(filterDTO.getRecipientId());
                    } else {
                        sb.append(" WHERE recipient_id = ").append(filterDTO.getRecipientId());
                    }
                }

                if (filterDTO.getStatus() != null && !filterDTO.getStatus().isEmpty() && filterDTO.getStatus().matches("-?\\d+")) {
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
                if (pageAndFilterDTO.getPageDto().getOrder() != null) {
                    if (("DESC".equalsIgnoreCase(pageAndFilterDTO.getPageDto().getOrder()) || "ASC".equalsIgnoreCase(pageAndFilterDTO.getPageDto().getOrder()))) {
                        StringBuilder sb = new StringBuilder(query);
                        sb.deleteCharAt(query.length() - 1);
                        sb.append(" ORDER BY request_id ").append(pageAndFilterDTO.getPageDto().getOrder().toUpperCase()).append(";");

                        query = sb.toString();
                    } else {
                        throw new WrongPageOrderException("Wrong page params");
                    }
                }

                if (pageAndFilterDTO.getPageDto().getLimit() != null && pageAndFilterDTO.getPageDto().getLimit() >= 1) {
                    StringBuilder sb = new StringBuilder(query);
                    if (sb.toString().contains(";")) {
                        sb.deleteCharAt(query.length() - 1);
                    }
                    sb.append(" LIMIT ").append(pageAndFilterDTO.getPageDto().getLimit()).append(";");
                    query = sb.toString();
                }
            }
        }

        System.out.println(query);

        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            while (resultSet.next()) {
                if (resultSet.getString(2) != null) {
                    WillRequestResponseDTO willRequest = getWillRequestDetails(resultSet.getString("request_id"));
                    willRequests.add(willRequest);
                }
            }
            resultSet.close();
            return willRequests;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {

        }
        return new ArrayList<>();
    }

    public static WillRequestResponseDTO getWillRequestDetails(@NotNull String willRequestId) throws IOException {
        loadProps();

//        String id;
//        String userId;
//        String recipientId;
//        String fullName;
//        String passportId;
//        String statusId;
//        String did;
//        String email;
//        String address;
//        String documentHash;
//        String documentLink;

        String query = "SELECT * FROM " + prop.getProperty(REQUESTS_TABLE) + " WHERE request_id= " + willRequestId + ";";
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();

            WillRequestResponseDTO willRequest = new WillRequestResponseDTO();
            willRequest.setId(resultSet.getString(1));
            willRequest.setStatusId(resultSet.getString(3));
            String documentId = resultSet.getString(4);

            String creatorId = resultSet.getString(2);
            String recipientId = resultSet.getString(5);


            UserModelDTO creator = new UserModelDTO();
            query = "SELECT email, did, kyc_id FROM " + prop.getProperty(USER_TABLE) + " WHERE user_id=" + creatorId + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            creator.setId(creatorId);
            creator.setEmail(resultSet.getString("email"));
            creator.setDid(resultSet.getString("did"));
            String creatorKycId = resultSet.getString("kyc_id");

            query = "SELECT * FROM " + prop.getProperty(KYC_TABLE) + " WHERE kyc_id=" + creatorKycId + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            creator.setFirstName(resultSet.getString("first_name"));
            creator.setLastName(resultSet.getString("last_name"));
            creator.setSecondName(resultSet.getString("second_name"));
            creator.setAddress(resultSet.getString("address"));
            creator.setNif(resultSet.getString("nif"));
            creator.setBirthday(resultSet.getString("birthday"));

            willRequest.setCreator(creator);

            UserModelDTO recipient = new UserModelDTO();
            query = "SELECT email, did, kyc_id FROM " + prop.getProperty(USER_TABLE) + " WHERE user_id=" + recipientId + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            recipient.setId(recipientId);
            recipient.setEmail(resultSet.getString("email"));
            recipient.setDid(resultSet.getString("did"));
            String recipientKycId = resultSet.getString("kyc_id");

            query = "SELECT * FROM " + prop.getProperty(KYC_TABLE) + " WHERE kyc_id=" + recipientKycId + ";";
            resultSet = connect().createStatement().executeQuery(query);
            resultSet.next();
            recipient.setFirstName(resultSet.getString("first_name"));
            recipient.setLastName(resultSet.getString("last_name"));
            recipient.setSecondName(resultSet.getString("second_name"));
            recipient.setAddress(resultSet.getString("address"));
            recipient.setNif(resultSet.getString("nif"));
            recipient.setBirthday(resultSet.getString("birthday"));

            willRequest.setRecipient(recipient);

            query = "SELECT * FROM " + prop.getProperty(DOCS_TABLE) + " WHERE document_id=" + documentId + ";";
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
        return null;
    }

    public static String createUser(UserRegistrationDTO user, String did) throws SQLException, IOException, InvalidRoleIdException, NoSuchAlgorithmException {
        String table;

        loadProps();
        //  Check draft user record
        Boolean draftMode = false;
        try (Connection connection = connect()) {
            String draftTestQuery = "SELECT password_hash FROM " + prop.getProperty(USER_TABLE) + " WHERE email = '"
                    + user.getEmail() + "';";
            ResultSet resultSet = connection.createStatement().executeQuery(draftTestQuery);
            if (resultSet.next()) {
                //  user exists
                String passwdHash = resultSet.getString(1);
                if (passwdHash == null) {
                    draftMode = true;
                }
            } else {
                // user not found - ok
            }
            resultSet.close();
        } catch (SQLException | IOException e) {
            log.severe(e.getMessage());
            throw e;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Salt salt = saltPassword(user.getPassword());

        StringBuilder query = new StringBuilder();
        if (!draftMode) {
            query.append("INSERT INTO ");
        } else {
            query.append("UPDATE ");
        }
        int roleId = Integer.parseInt(user.getRole());

        switch (roleId) {
            case 0:
                query.append(prop.getProperty(USER_TABLE)).append(" SET ");
                break;
            case 1:
                query.append(prop.getProperty(USER_TABLE)).append(" SET ");
                query.append("public_key=\"").append(DSM.encodePK(DSM.generateKeyPair(user.getPassword().getBytes(StandardCharsets.UTF_8)).getPublic())).append("\","
                ).append("did=\"").append(did).append("\",");
                break;
            case 2:
                query.append(prop.getProperty(USER_TABLE)).append(" SET ");
                query.append("public_key=\"").append(DSM.encodePK(DSM.generateKeyPair(user.getPassword().getBytes(StandardCharsets.UTF_8)).getPublic())).append("\","
                ).append("did=\"").append(did).append("\",");
                break;
            default:
                throw new InvalidRoleIdException("Such role does not exist!");
        }

        query.append(" email=\"" + user.getEmail() + "\","
                + "password_hash=\"" + salt.getSaltedPassword() + "\","
                + "salt=\"" + salt.getSalt() + "\","
                + "role_id=" + roleId);

        if (draftMode) {
            query.append(" WHERE email = '" + user.getEmail() + "';");
        } else {
            query.append(";");
        }


        log.log(Level.INFO, "Executing query {0}", query.toString());

        executeUpdateToDB(query.toString());

//        'kyc_id',
        String queryKyc = "INSERT INTO " + prop.getProperty(KYC_TABLE) + "(first_name, last_name, second_name, address, nif, birthday) VALUES (\"\", \"\", \"\", \"\", \"\", \"\");";

        executeUpdateToDB(queryKyc);

        String kycQuery = "SELECT kyc_id FROM kyc ORDER BY kyc_id DESC";

        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(kycQuery);
            resultSet.next();
            String assignKyc = "UPDATE users SET kyc_id = " + resultSet.getString(1) + " WHERE user_id = " + getUserId(user.getEmail());
            executeUpdateToDB(assignKyc);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return getUserId(user.getEmail());

    }

    public static void createNotaryRegistry(NotaryRegistryDTO user) throws IOException {
        loadProps();
        String table;
        Salt salt = saltPassword(user.getPassword());

        if (Integer.parseInt(user.getRoleId()) == 1) {
            table = prop.getProperty(NOTARY_TABLE);
        } else {
            table = prop.getProperty(REGISTRY_TABLE);
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
        String query = "SELECT password_hash, salt FROM " + prop.getProperty(USER_TABLE) + " WHERE email=\"" + user.getEmail() + "\";";
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

        String query = "SELECT email FROM " + prop.getProperty(USER_TABLE) +
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

        String setQuery = "INSERT INTO " + prop.getProperty(STATUSES_TABLE) + " SET " +
                "user_id=\"" + getUserId(serviceStatus.getUserEmail()) + "\","
                + "institution=\"" + serviceStatus.getInstitution() + "\","
                + "service=\"" + serviceStatus.getService() + "\","
                + "status=\"" + serviceStatus.getStatus() + "\";";
        System.out.println("Executing query: " + setQuery);
        executeUpdateToDB(setQuery);
    }

    public static void addKYC(KycDTO kyc) throws IOException {
        loadProps();

        String query = "UPDATE " + prop.getProperty(KYC_TABLE) + " SET "
                + "first_name=\"" + kyc.getFirstName() + "\","
                + "last_name=\"" + kyc.getLastName() + "\","
                + "second_name=\"" + kyc.getSecondName() + "\","
                + "address=\"" + kyc.getAddress() + "\","
                + "nif=\"" + kyc.getNif() + "\","
                + "birthday=\"" + kyc.getBirthday() + "\""
                + " WHERE kyc_id=" + getKYCId(getUserId(kyc.getEmail())) + ";";
        System.out.println("Executing query: " + query);
        executeUpdateToDB(query);
    }

    public static void storeQuestions(QuestionsDTO questionsDTO) throws IOException {
        loadProps();

        String query = "INSERT INTO " + prop.getProperty("questionstable") + " SET "
                + "user_id=\"" + getUserId(questionsDTO.getEmail()) + "\","
                + "q1=\"" + questionsDTO.getQ1() + "\","
                + "q1_18y=\"" + questionsDTO.getQ1_18() + "\","
                + "q2=\"" + questionsDTO.getQ2() + "\","
                + "q3=\"" + questionsDTO.getQ3() + "\","
                + "q4=\"" + questionsDTO.getQ4() + "\","
                + "q5=\"" + questionsDTO.getQ5() + "\";";
        System.out.println("Executing query: " + query);
        executeUpdateToDB(query);
    }

    private static String getKYCId(String userId) throws IOException {
        loadProps();
        String query = "SELECT kyc_id FROM " + prop.getProperty(USER_TABLE) + " WHERE user_id = " + userId + ";";
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();
            return resultSet.getString(1);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static UserModelDTO getUser(String userId) throws IOException, UserNotFoundException, KycNotFoundException {
        loadProps();

        String query = "SELECT * FROM " + prop.getProperty(USER_TABLE) + " WHERE user_id= " + userId + ";";
        System.out.println("Get user query -> " + query);
        try (Connection connection = connect()) {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            if (resultSet.next()) {
                String email = resultSet.getString(2);
                String kycId = resultSet.getString("kyc_id");
                resultSet.close();
                return new UserModelDTO(userId, email, getUserKyc(kycId));
            } else {
                throw new UserNotFoundException("No user was found for this userId -> " + userId);
            }
        } catch (SQLException | IOException | ClassNotFoundException | KycNotFoundException e) {
            e.printStackTrace();
            if (e instanceof KycNotFoundException) {
                throw new KycNotFoundException("No userKyc was found for this userId -> " + userId);
            } else {
                throw new UserNotFoundException("No user was found for this userId -> " + userId);
            }
        }
    }

    private static KycDTO getUserKyc(String kycId) throws IOException, KycNotFoundException {
        loadProps();

        String query = "SELECT * FROM " + prop.getProperty(KYC_TABLE) + " WHERE kyc_id= " + kycId + ";";
        System.out.println("Get kyc query -> " + query);

        try (Connection connection = connect()) {
            System.out.println("Executing query: " + query);
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            if (resultSet.next()) {
                String first_name = resultSet.getString("first_name");
                String last_name = resultSet.getString("last_name");
                String second_name = resultSet.getString("second_name");
                String address = resultSet.getString("address");
                String nif = resultSet.getString("nif");
                String birthday = resultSet.getString("birthday");
                resultSet.close();
                return new KycDTO(first_name,
                        last_name,
                        second_name,
                        address,
                        nif,
                        birthday
                );
            } else {
                throw new KycNotFoundException("No user was found for this userId -> " + kycId);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new KycNotFoundException("No user was found for this userId -> " + kycId);
        }

    }

    private static String getUserPassword(String email) throws IOException {
        loadProps();
        String query = "SELECT password_hash FROM " + prop.getProperty(USER_TABLE) + " WHERE id =" +
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
        String query = "SELECT * FROM " + prop.getProperty(STATUSES_TABLE) + " WHERE " +
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
        String getUserIdQuery = "SELECT user_id FROM " + prop.getProperty(USER_TABLE) + " WHERE email=\""
                + userEmail + "\";";
        System.out.println("Executing query: " + getUserIdQuery);
        try (Connection connection = connect()) {
            ResultSet resultSet = connection.createStatement().executeQuery(getUserIdQuery);
            if (resultSet.next()) {
                String userId = resultSet.getString(1);
                resultSet.close();
                System.out.println("User id -> " + userId);
                return userId;
            } else {
                throw new UserNotFoundException("There is no such user");
            }
        } catch (SQLException | IOException | ClassNotFoundException | UserNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String login(UserLoginDTO user) throws IOException, SQLException, ClassNotFoundException {
        loadProps();
        if (isEmailExists(user) && isPasswordValid(user)) {
            String query = "SELECT user_id, role_id FROM " + prop.getProperty(USER_TABLE) + " WHERE email=\"" + user.getEmail() + "\";";
            System.out.println("Executing query: " + query);
            Connection connection = connect();
            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();
            String jwt = generateJWT(user, resultSet.getString(1), resultSet.getString(2));
            resultSet.close();
            return jwt;

        }
        return null;
    }

    public static String notaryRegistryLogin(NotaryRegistryLoginDTO user) throws IOException {
        loadProps();
        String table, columnName;

        if (Integer.parseInt(user.getRoleId()) == 1) {
            table = prop.getProperty(NOTARY_TABLE);
            columnName = "notary_id";
        } else {
            table = prop.getProperty(REGISTRY_TABLE);
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
                    .withClaim("role", accountTypeId)
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
                    .put("role", jwt.getClaim("role").asString())
                    .put("user_id", jwt.getClaim("user_id").asString())
                    .put("email", jwt.getClaim("email").asString());
            String JWTClaimsJSON = json.toString();
            return JWTClaimsJSON;
        } catch (JWTDecodeException e) {
            return null;
        }
    }

    public static void sendNotification(String willId) throws IOException {
        loadProps();

        String recipientEmail;
        String recipientFirstName;
        String recipientLastName;
        String documentId;
        String documentUrl;

        String query = "SELECT * FROM " + prop.getProperty(REQUESTS_TABLE) + " WHERE request_id =" + willId + ";";
        try (Connection connection = connect()) {

            String statusQuery = "UPDATE " + prop.getProperty(REQUESTS_TABLE) + " SET status_id = 3 WHERE request_id =" + willId + ";";
            executeUpdateToDB(statusQuery);

            ResultSet resultSet = connection.createStatement().executeQuery(query);
            resultSet.next();
            String creatorId = resultSet.getString(2);
            documentId = resultSet.getString(2);
            String recipientId = resultSet.getString(5);

            String userQuery = "SELECT * FROM " + prop.getProperty(USER_TABLE) + " WHERE user_id =" + recipientId + ";";
            resultSet = connection.createStatement().executeQuery(userQuery);
            resultSet.next();
            recipientEmail = resultSet.getString(2);
            String kycId = resultSet.getString(6);

            String kycQuery = "SELECT * FROM " + prop.getProperty(KYC_TABLE) + " WHERE kyc_id =" + kycId + ";";
            resultSet = connection.createStatement().executeQuery(kycQuery);
            resultSet.next();
            recipientFirstName = resultSet.getString(2);
            recipientLastName = resultSet.getString(4);

            String docQuery = "SELECT * FROM " + prop.getProperty(DOCS_TABLE) + " WHERE document_id =" + documentId + ";";
            resultSet = connection.createStatement().executeQuery(docQuery);
            resultSet.next();
            documentUrl = resultSet.getString(3);

            try {
                EmailService.sendEmail(recipientEmail, "kostyanich7@gmail.com",
                        "TestSubject", MailContentBuilder.generateMailContent(
                                new EmailPayload("Test Mail",
                                        "kostiantyn.nechvolod@gmail.com",
                                        recipientFirstName,
                                        recipientLastName,
                                        documentUrl
                                )
                        ),
                        // TODO Generate pdf and provide it's content
                        null);
            } catch (NullPointerException e) {
                throw new IllegalStateException("JSON payload structure is incorrect");
            }

        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void confirmDeath(String willId) throws IOException {

        loadProps();

        try (Connection connection = connect()) {
            String statusQuery = "UPDATE " + prop.getProperty(REQUESTS_TABLE) + " SET status_id = 2 WHERE request_id =" + willId + ";";
            executeUpdateToDB(statusQuery);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void deleteWill(String willId) throws IOException {
        loadProps();
        String query = "UPDATE " + prop.getProperty(REQUESTS_TABLE) + " SET status_id=-3 WHERE request_id=" + willId + ";";
        log.log(Level.INFO, "Executing query {0}", query);
        executeUpdateToDB(query);
    }
}