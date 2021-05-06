package com.liv.cryptomodule.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private EmailService() {
        throw new IllegalStateException("Utility class");
    }

    private static Properties prop = new Properties();

    private static void loadProps() throws IOException {
        InputStream input = EmailService.class.getClassLoader().getResourceAsStream("application.properties");
        if (input == null) {
            log.log(Level.SEVERE, "Can''t to find application.properties");
            return;
        }
        prop.load(input);
    }

    private static final Logger log = java.util.logging.Logger.getLogger(EmailService.class.getName());
    private static JavaMailSender mail = null;

    public static void initializeEmailService() {
        if (mail == null) {
            mail = getJavaMailSender();
        } else {
            log.log(Level.INFO, "Mail Sender already initialized!");
        }

    }

    public static Boolean sendEmail(String to, String from, String subject, String text, byte[] pdfBytes) throws IOException{
        loadProps();

        if (mail == null) {
            log.log(Level.WARNING, "Mail Sender not initialized!");
            initializeEmailService();
        }

        String[] recepientsArray = to.split(",");

        MimeMessage message = mail.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true);

            helper.setFrom(from);
            helper.setTo(recepientsArray);
            helper.setSubject(subject);
            helper.setText(text, true);
            if(pdfBytes != null){
                helper.addAttachment("document.pdf", new ByteArrayDataSource(pdfBytes, "application/pdf"));
            }
            mail.send(message);

            log.log(Level.INFO, "Mail sent to: {0}", to);

            return true;

        } catch (MessagingException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
        return false;
    }

    public static JavaMailSender getJavaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(prop.getProperty("spring.mail.host"));
        mailSender.setPort(Integer.parseInt(prop.getProperty("spring.mail.port")));

        mailSender.setUsername(prop.getProperty("spring.mail.username"));
        mailSender.setPassword(prop.getProperty("spring.mail.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
