package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import java.io.IOException;

public class EmailReputationService {

    private static final String API_KEY = tn.esprit.utils.Config.getEmailKey();
    private static final String BASE_URL = "https://emailreputation.abstractapi.com/v1/";
    private final Gson gson = new Gson();

    /**
     * Vérifie la réputation et la validité d'un email.
     * @param email L'adresse email à vérifier.
     * @return Un objet EmailReputation contenant les infos.
     */
    public EmailReputation checkEmailReputation(String email) {
        try {
            // Construire l'URL avec l'email et la clé API
            String url = BASE_URL + "?api_key=" + API_KEY + "&email=" + email;

            // Faire la requête HTTP GET
            Content content = Request.Get(url)
                    .connectTimeout(5000)
                    .socketTimeout(5000)
                    .execute()
                    .returnContent();

            // Parser la réponse JSON
            JsonObject jsonResponse = gson.fromJson(content.asString(), JsonObject.class);
            return parseReputation(jsonResponse);

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'appel API pour l'email " + email + " : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse la réponse JSON de l'API.
     */
    private EmailReputation parseReputation(JsonObject json) {
        EmailReputation reputation = new EmailReputation();

        // --- Email Deliverability ---
        JsonObject deliverability = json.getAsJsonObject("email_deliverability");
        reputation.isFormatValid = deliverability.get("is_format_valid").getAsBoolean();
        reputation.isSmtpValid = deliverability.get("is_smtp_valid").getAsBoolean();
        reputation.isMxValid = deliverability.get("is_mx_valid").getAsBoolean();
        reputation.statusDetail = deliverability.get("status_detail").getAsString();

        // --- Email Quality ---
        JsonObject quality = json.getAsJsonObject("email_quality");
        reputation.score = quality.get("score").getAsDouble();
        reputation.isFreeEmail = quality.get("is_free_email").getAsBoolean();
        reputation.isDisposable = quality.get("is_disposable").getAsBoolean();
        reputation.isCatchall = quality.get("is_catchall").getAsBoolean();
        reputation.isRoleBased = quality.get("is_role").getAsBoolean();

        // --- Email Breaches (optionnel, peut être null) ---
        JsonObject breaches = json.getAsJsonObject("email_breaches");
        if (breaches != null && !breaches.isJsonNull()) {
            reputation.totalBreaches = breaches.has("total_breaches") && !breaches.get("total_breaches").isJsonNull()
                    ? breaches.get("total_breaches").getAsInt() : 0;
        } else {
            reputation.totalBreaches = 0;
        }

        return reputation;
    }

    /**
     * Classe interne pour structurer les résultats.
     */
    public static class EmailReputation {
        private boolean isFormatValid;
        private boolean isSmtpValid;
        private boolean isMxValid;
        private String statusDetail;
        private double score;
        private boolean isFreeEmail;
        private boolean isDisposable;
        private boolean isCatchall;
        private boolean isRoleBased;
        private int totalBreaches;

        // Getters pour accéder aux données depuis l'UI
        public boolean isFormatValid() { return isFormatValid; }
        public boolean isSmtpValid() { return isSmtpValid; }
        public boolean isMxValid() { return isMxValid; }
        public String getStatusDetail() { return statusDetail; }
        public double getScore() { return score; }
        public boolean isFreeEmail() { return isFreeEmail; }
        public boolean isDisposable() { return isDisposable; }
        public boolean isCatchall() { return isCatchall; }
        public boolean isRoleBased() { return isRoleBased; }
        public int getTotalBreaches() { return totalBreaches; }

        /**
         * Méthode utilitaire pour obtenir un résumé lisible dans l'UI.
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (isFormatValid && isSmtpValid) {
                sb.append("✅ Email valide et délivrable.\n");
            } else {
                sb.append("❌ Email invalide : ").append(statusDetail).append("\n");
            }

            if (totalBreaches > 0) {
                sb.append("⚠️ Trouvé dans ").append(totalBreaches).append(" fuite(s) de données.\n");
            }

            if (isDisposable) {
                sb.append("⚠️ Email jetable détecté.\n");
            }
            if (isRoleBased) {
                sb.append("ℹ️ Email générique (ex: contact@).\n");
            }
            if (isFreeEmail) {
                sb.append("ℹ️ Fournisseur gratuit (Gmail, etc.).\n");
            }

            sb.append("📊 Score de qualité : ").append(String.format("%.1f", score * 100)).append("%");
            return sb.toString();
        }
    }
}