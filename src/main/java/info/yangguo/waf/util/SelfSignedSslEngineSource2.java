package info.yangguo.waf.util;

import com.google.common.io.ByteStreams;
import org.littleshoot.proxy.SslEngineSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author:杨果
 * @date:2017/5/31 下午3:35
 * <p>
 * Description:
 */
public class SelfSignedSslEngineSource2 implements SslEngineSource {
    private static final Logger LOG = LoggerFactory
            .getLogger(SelfSignedSslEngineSource2.class);

    private static String keyStoreName = "waf-keystore.jks";
    private static final String ALIAS = "waf";
    private static String PASSWORD = "yangguo";
    private static final String PROTOCOL = "TLS";
    private final boolean trustAllServers;
    private final boolean sendCerts;
    private final String cn;

    private SSLContext sslContext;

    public SelfSignedSslEngineSource2(String keyStoreName,
                                      boolean trustAllServers, boolean sendCerts, String password,String cn) {
        this.trustAllServers = trustAllServers;
        this.sendCerts = sendCerts;
        this.keyStoreName = keyStoreName;
        this.PASSWORD=password;
        this.cn=cn;
        initializeKeyStore();
        this.initializeSSLContext();
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

    private void initializeKeyStore() {
        String keyStorePath=this.getClass().getResource("/").getPath()+keyStoreName;
        String certPath=this.getClass().getResource("/").getPath()+"waf_cert";
        File keyStoreFile=new File(keyStorePath);
        if (keyStoreFile.isFile() && keyStoreFile.exists()) {
            LOG.info("Not deleting keystore");
            return;
        }

        StringBuilder dname=new StringBuilder()
                .append("CN=").append(cn)
                .append(", ").append("OU=").append(cn)
                .append(", ").append("O=").append(cn)
                .append(", L=SH, ST=SH, C=CN");
        nativeCall("keytool", "-genkey", "-alias", ALIAS, "-keysize",
                "4096", "-validity", "36500", "-keyalg", "RSA", "-dname",
                dname.toString(), "-keypass", PASSWORD, "-storepass",
                PASSWORD, "-keystore", keyStorePath);

        nativeCall("keytool", "-exportcert", "-alias", ALIAS, "-keystore",
                keyStorePath, "-storepass", PASSWORD, "-file",
                certPath);
    }

    private void initializeSSLContext() {
        String algorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            // ks.load(new FileInputStream("keystore.jks"),
            // "changeit".toCharArray());
            ks.load(this.getClass().getResourceAsStream(keyStoreName), PASSWORD.toCharArray());

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

    private String nativeCall(final String... commands) {
        LOG.info("Running '{}'", Arrays.asList(commands));
        final ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            final Process process = pb.start();
            final InputStream is = process.getInputStream();

            byte[] data = ByteStreams.toByteArray(is);
            String dataAsString = new String(data);

            LOG.info("Completed native call: '{}'\nResponse: '" + dataAsString + "'",
                    Arrays.asList(commands));
            return dataAsString;
        } catch (final IOException e) {
            LOG.error("Error running commands: " + Arrays.asList(commands), e);
            return "";
        }
    }

}
