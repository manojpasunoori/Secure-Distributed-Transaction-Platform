package com.sdtp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {
    private Tls tls = new Tls();
    private List<ValidatorNodeProperties> validators = new ArrayList<>();

    public Tls getTls() {
        return tls;
    }

    public void setTls(Tls tls) {
        this.tls = tls;
    }

    public List<ValidatorNodeProperties> getValidators() {
        return validators;
    }

    public void setValidators(List<ValidatorNodeProperties> validators) {
        this.validators = validators;
    }

    public static class Tls {
        private String caCert;
        private String clientKeystore;
        private String clientKeystorePassword;
        private String clientKeystoreType = "PKCS12";

        public String getCaCert() {
            return caCert;
        }

        public void setCaCert(String caCert) {
            this.caCert = caCert;
        }

        public String getClientKeystore() {
            return clientKeystore;
        }

        public void setClientKeystore(String clientKeystore) {
            this.clientKeystore = clientKeystore;
        }

        public String getClientKeystorePassword() {
            return clientKeystorePassword;
        }

        public void setClientKeystorePassword(String clientKeystorePassword) {
            this.clientKeystorePassword = clientKeystorePassword;
        }

        public String getClientKeystoreType() {
            return clientKeystoreType;
        }

        public void setClientKeystoreType(String clientKeystoreType) {
            this.clientKeystoreType = clientKeystoreType;
        }
    }
}
