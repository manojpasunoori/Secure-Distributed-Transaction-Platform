package com.sdtp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdtp.config.PlatformProperties;
import com.sdtp.config.ValidatorNodeProperties;
import com.sdtp.model.TransactionRequest;
import com.sdtp.model.ValidatorDecision;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
public class ValidatorTlsClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidatorDecision validate(ValidatorNodeProperties node,
                                      TransactionRequest request,
                                      PlatformProperties properties) {
        try {
            SSLContext sslContext = buildSslContext(properties.getTls());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(node.host(), node.port())) {
                SSLParameters sslParameters = socket.getSSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                socket.setSSLParameters(sslParameters);
                socket.startHandshake();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer.write(objectMapper.writeValueAsString(request));
                writer.newLine();
                writer.flush();

                String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    return new ValidatorDecision(node.id(), "REJECT", "empty validator response");
                }
                return objectMapper.readValue(line, ValidatorDecision.class);
            }
        } catch (Exception e) {
            return new ValidatorDecision(node.id(), "REJECT", "validator communication failure: " + e.getMessage());
        }
    }

    private SSLContext buildSslContext(PlatformProperties.Tls tls) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (InputStream caInput = Files.newInputStream(Path.of(tls.getCaCert()))) {
            caCert = (X509Certificate) certificateFactory.generateCertificate(caInput);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyManager[] keyManagers = null;
        if (tls.getClientKeystore() != null && !tls.getClientKeystore().isBlank()) {
            KeyStore keyStore = KeyStore.getInstance(tls.getClientKeystoreType());
            try (InputStream keyStoreStream = Files.newInputStream(Path.of(tls.getClientKeystore()))) {
                char[] pwd = tls.getClientKeystorePassword().toCharArray();
                keyStore.load(keyStoreStream, pwd);
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, pwd);
                keyManagers = kmf.getKeyManagers();
            }
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, tmf.getTrustManagers(), null);
        return sslContext;
    }
}
