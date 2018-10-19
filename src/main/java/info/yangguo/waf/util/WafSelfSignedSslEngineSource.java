/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.yangguo.waf.util;

import net.lightbody.bmp.mitm.CertificateInfo;
import net.lightbody.bmp.mitm.RootCertificateGenerator;
import net.lightbody.bmp.mitm.keys.ECKeyGenerator;
import net.lightbody.bmp.mitm.keys.RSAKeyGenerator;
import org.joda.time.DateTime;
import org.littleshoot.proxy.SslEngineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author:杨果
 * @date:2017/5/31 下午3:35
 * <p>
 * Description:
 */
public class WafSelfSignedSslEngineSource implements SslEngineSource {
    private static final Logger logger = LoggerFactory
            .getLogger(org.littleshoot.proxy.extras.SelfSignedSslEngineSource.class);

    private static final String ALIAS = "waf";
    public static final String PASSWORD = "yangguo";
    private static final String PROTOCOL = "TLS";
    public static final String crtFileName = "waf.crt";
    public static final String jksKeyStoreFileName = "waf.jks";
    public static final String p12KeyStoreFileName = "waf.p12";
    private static String KEYALG = "EC";
    public File keyStoreFile;
    private final boolean trustAllServers;
    private final boolean sendCerts;
    private KeyStoreType keyStoreType;
    private SSLContext sslContext;

    /**
     * use exist keystore
     *
     * @param keyStorePath
     * @param trustAllServers
     * @param sendCerts
     */
    public WafSelfSignedSslEngineSource(String keyStorePath,
                                        boolean trustAllServers, boolean sendCerts, KeyStoreType keyStoreType) {
        JCEUtil.removeCryptographyRestrictions();
        this.trustAllServers = trustAllServers;
        this.sendCerts = sendCerts;
        this.keyStoreFile = new File(keyStorePath);
        this.keyStoreType = keyStoreType;
        initializeSSLContext();
    }

    /**
     * create keystore
     *
     * @param trustAllServers
     * @param sendCerts
     */
    public WafSelfSignedSslEngineSource(boolean trustAllServers, boolean sendCerts, String keyalg) {
        JCEUtil.removeCryptographyRestrictions();
        this.trustAllServers = trustAllServers;
        this.sendCerts = sendCerts;
        this.keyStoreType = KeyStoreType.JKS;
        this.KEYALG = keyalg;
        initializeKeyStore();
        initializeSSLContext();
    }

    public WafSelfSignedSslEngineSource() {
        this(false, true, KEYALG);
    }

    @Override
    public SSLEngine newSslEngine() {
        return sslContext.createSSLEngine();
    }

    @Override
    public SSLEngine newSslEngine(String peerHost, int peerPort) {
        return sslContext.createSSLEngine(peerHost, peerPort);
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void initializeKeyStore() {
        File crtFile = new File(crtFileName);
        File jksFile = new File(jksKeyStoreFileName);
        File p12File = new File(p12KeyStoreFileName);
        keyStoreFile = jksFile;
        if (keyStoreFile.isFile()) {
            logger.info("Not deleting keystore");
            return;
        }

        CertificateInfo certificateInfo = new CertificateInfo();
        certificateInfo.countryCode("CN");
        certificateInfo.organization("yangguo.info");
        certificateInfo.email("yangguo@outlook.com");
        certificateInfo.commonName("WAF Integration Certification Authority");
        DateTime dateTime = new DateTime();
        certificateInfo.notBefore(dateTime.minusDays(1).toDate());
        certificateInfo.notAfter(dateTime.plusYears(1).toDate());
        RootCertificateGenerator.Builder rootCertificateGeneratorBuilder = RootCertificateGenerator.builder();
        rootCertificateGeneratorBuilder.certificateInfo(certificateInfo);
        RootCertificateGenerator rootCertificateGenerator;
        if (KEYALG.equals("RSA")) {
            rootCertificateGenerator = rootCertificateGeneratorBuilder.keyGenerator(new RSAKeyGenerator()).build();
        } else {
            rootCertificateGenerator = rootCertificateGeneratorBuilder.keyGenerator(new ECKeyGenerator()).build();
        }

        rootCertificateGenerator.saveRootCertificateAsPemFile(crtFile);
        logger.info("CRT file created success");
        rootCertificateGenerator.saveRootCertificateAndKey(KeyStoreType.JKS.name(), jksFile, ALIAS, PASSWORD);
        logger.info("JKS file created success");
        rootCertificateGenerator.saveRootCertificateAndKey(KeyStoreType.PKCS12.name(), p12File, ALIAS, PASSWORD);
        logger.info("PKCS12 file created success");
    }

    private void initializeSSLContext() {
        String algorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        try {
            final KeyStore ks = KeyStore.getInstance(keyStoreType.name());
            ks.load(new FileInputStream(keyStoreFile), PASSWORD.toCharArray());

            // Set up key manager factory to use our key store
            final KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, PASSWORD.toCharArray());

            // Set up a trust manager factory to use our key store
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(algorithm);
            tmf.init(ks);

            TrustManager[] trustManagers = null;
            if (!trustAllServers) {
                trustManagers = tmf.getTrustManagers();
            } else {
                trustManagers = new TrustManager[]{new X509TrustManager() {
                    // TrustManager that trusts all servers
                    @Override
                    public void checkClientTrusted(X509Certificate[] arg0,
                                                   String arg1)
                            throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] arg0,
                                                   String arg1)
                            throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }};
            }

            KeyManager[] keyManagers = null;
            if (sendCerts) {
                keyManagers = kmf.getKeyManagers();
            } else {
                keyManagers = new KeyManager[0];
            }

            // Initialize the SSLContext to work with our key managers.
            sslContext = SSLContext.getInstance(PROTOCOL);
            sslContext.init(keyManagers, trustManagers, null);
        } catch (final Exception e) {
            throw new Error(
                    "Failed to initialize the server-side SSLContext", e);
        }
    }

    public enum KeyStoreType {
        JKS, PKCS12;
    }
}
