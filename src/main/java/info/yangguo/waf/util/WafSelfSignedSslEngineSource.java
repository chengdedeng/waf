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
    private static final Logger LOG = LoggerFactory
            .getLogger(org.littleshoot.proxy.extras.SelfSignedSslEngineSource.class);

    private static final String ALIAS = "waf";
    public static final String PASSWORD = "yangguo";
    private static final String PROTOCOL = "TLS";
    private static String KEYALG = "EC";
    public final File keyStoreFile;
    private final boolean trustAllServers;
    private final boolean sendCerts;

    private SSLContext sslContext;

    public WafSelfSignedSslEngineSource(String keyStorePath,
                                        boolean trustAllServers, boolean sendCerts, String keyalg) {
        this.trustAllServers = trustAllServers;
        this.sendCerts = sendCerts;
        this.keyStoreFile = new File(keyStorePath);
        this.KEYALG = keyalg;
        initializeKeyStore();
        initializeSSLContext();
    }

    public WafSelfSignedSslEngineSource(String keyStorePath,
                                        boolean trustAllServers, boolean sendCerts) {
        this.trustAllServers = trustAllServers;
        this.sendCerts = sendCerts;
        this.keyStoreFile = new File(keyStorePath);
        initializeKeyStore();
        initializeSSLContext();
    }

    public WafSelfSignedSslEngineSource(String keyStorePath) {
        this(keyStorePath, false, true);
    }

    public WafSelfSignedSslEngineSource(boolean trustAllServers) {
        this(trustAllServers, true);
    }

    public WafSelfSignedSslEngineSource(boolean trustAllServers, boolean sendCerts) {
        this("waf_keystore.jks", trustAllServers, sendCerts);
    }

    public WafSelfSignedSslEngineSource() {
        this(false);
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
        if (keyStoreFile.isFile()) {
            LOG.info("Not deleting keystore");
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

        rootCertificateGenerator.saveRootCertificateAsPemFile(new File("waf_cert"));
        rootCertificateGenerator.saveRootCertificateAndKey("JKS", keyStoreFile, ALIAS, PASSWORD);
    }

    private void initializeSSLContext() {
        String algorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
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
}
