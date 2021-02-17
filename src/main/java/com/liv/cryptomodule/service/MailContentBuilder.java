package com.liv.cryptomodule.service;

import com.liv.cryptomodule.payload.EmailPayload;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MailContentBuilder {

    private MailContentBuilder() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger log = java.util.logging.Logger.getLogger(MailContentBuilder.class.getName());

    private static TemplateEngine templateEngine = null;

    public static String generateMailContent(EmailPayload payload) {

        if (templateEngine == null) {
            log.log(Level.WARNING, "Template Engine not initialized!");
            initializeHtmlTemplateEngine();
        }

        String templateName = "template-english";
        Context context = new Context();
        context.setVariable("recepientFirstName", payload.getRecipientFirstName());
        context.setVariable("recepientLastName", payload.getRecipientLastName());
        context.setVariable("documentLink", payload.getDocumentLink());

        return templateEngine.process(templateName, context);
    }

    public static void initializeHtmlTemplateEngine() {
        if (templateEngine == null) {
            TemplateEngine htmlTemplateEngine = new TemplateEngine();
            htmlTemplateEngine.addTemplateResolver(htmlTemplateResolver());
            templateEngine = htmlTemplateEngine;
        } else {
            log.log(Level.WARNING, "Template Engine already initialized!");
        }
    }

    private static ITemplateResolver htmlTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(2);
        templateResolver.setPrefix("/mail-templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        return templateResolver;
    }
}
