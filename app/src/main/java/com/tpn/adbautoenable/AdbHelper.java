package com.tpn.adbautoenable;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbStream;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import org.conscrypt.Conscrypt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

public class AdbHelper {
    private static final String TAG = "ADBAutoEnable";
    private final Context context;

    public AdbHelper(Context context) {
        this.context = context;  // ← Assign FIRST, outside try-catch

        try {
            // Install security providers
            Log.i(TAG, "Installing security providers");
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            Log.i(TAG, "AdbHelper initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize security providers", e);
            // Don't throw - providers might work later or may already be initialized
        }
    }

    public boolean pair(String host, int port, String code) {
        SimpleAdbManager manager = null;
        try {
            Log.i(TAG, "Pairing with " + host + ":" + port + " using code: " + code);
            manager = new SimpleAdbManager(context);
            manager.pair(host, port, code);
            Log.i(TAG, "Pairing successful!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Pairing failed", e);
            return false;
        } finally {
            if (manager != null) {
                try {
                    manager.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing manager after pair", e);
                }
            }
        }
    }

    public boolean connect(String host, int port) {
        SimpleAdbManager manager = null;
        try {
            manager = new SimpleAdbManager(context);
            manager.connect(host, port);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Connect failed", e);
            return false;
        } finally {
            if (manager != null) {
                try {
                    manager.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing manager after connect", e);
                }
            }
        }
    }

    public boolean selfGrantPermission(String host, int port, String packageName, String permission) {
        SimpleAdbManager manager = null;
        try {
            Log.i(TAG, "Attempting to grant permission " + permission + " to " + packageName);
            manager = new SimpleAdbManager(context);
            manager.connect(host, port);
            Log.i(TAG, "Connected, sending pm grant shell command");

            // Check if already granted
            if (checkPermissionGranted(manager, permission)) {
                Log.i(TAG, "Permission " + permission + " is already granted, skipping grant");
                return true;
            }

            String command = "shell:pm grant " + packageName + " " + permission;
            try (AdbStream stream = manager.openStream(command)) {
                // Stream automatically closed
            }
            Log.i(TAG, "Successfully granted permission!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to grant permission", e);
            return false;
        } finally {
            if (manager != null) {
                try {
                    manager.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing manager after selfGrant", e);
                }
            }
        }
    }

    public boolean switchToPort5555(String host, int port) {
        SimpleAdbManager manager = null;
        try {
            Log.i(TAG, "switchToPort5555: Starting with host=" + host + ", port=" + port);
            manager = new SimpleAdbManager(context);

            Log.i(TAG, "switchToPort5555: Calling connect(" + host + ":" + port + ")");
            manager.connect(host, port);
            Log.i(TAG, "switchToPort5555: connect() completed successfully");

            Log.i(TAG, "switchToPort5555: Waiting for connection to stabilize");
            Thread.sleep(200);

            Log.i(TAG, "switchToPort5555: Sending tcpip:5555 service command");
            try (AdbStream stream = manager.openStream("tcpip:5555");
                 InputStream inputStream = stream.openInputStream()) {

                Log.i(TAG, "switchToPort5555: Reading response from stream");
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);

                if (bytesRead > 0) {
                    String response = new String(buffer, 0, bytesRead);
                    Log.i(TAG, "switchToPort5555: Response received (" + bytesRead + " bytes): " + response);
                } else {
                    Log.i(TAG, "switchToPort5555: No response data received");
                }
            }

            Log.i(TAG, "switchToPort5555: Waiting 3000ms for ADB to restart on port 5555");
            Thread.sleep(3000);

            Log.i(TAG, "switchToPort5555: Successfully switched to port 5555");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "switchToPort5555: Failed to switch to port 5555", e);
            return false;
        } finally {
            if (manager != null) {
                try {
                    manager.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing manager after switchToPort5555", e);
                }
            }
        }
    }

    private boolean checkPermissionGranted(SimpleAdbManager manager, String permission) {
        try {
            Log.i(TAG, "Checking if permission is granted: " + permission);
            String command = "shell:dumpsys package com.tpn.adbautoenable | grep " + permission;

            try (AdbStream stream = manager.openStream(command);
                 InputStream inputStream = stream.openInputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                String response = (bytesRead > 0) ? new String(buffer, 0, bytesRead) : "";

                Log.i(TAG, "Permission check response: " + response);
                boolean isGranted = response.contains("granted=true");
                Log.i(TAG, "Permission " + permission + " is granted: " + isGranted);
                return isGranted;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permission", e);
            return false;
        }
    }

    private static class SimpleAdbManager extends AbsAdbConnectionManager {
        private PrivateKey privateKey;
        private PublicKey publicKey;
        private X509Certificate certificate;
        private final File keyFile;
        private final File pubKeyFile;
        private final File certFile;

        public SimpleAdbManager(Context context) throws Exception {
            Log.i(TAG, "SimpleAdbManager constructor starting");
            setApi(Build.VERSION.SDK_INT);
            keyFile = new File(context.getFilesDir(), "adb_key");
            pubKeyFile = new File(context.getFilesDir(), "adb_key.pub");
            certFile = new File(context.getFilesDir(), "adb_cert");
            Log.i(TAG, "Key files: " + keyFile.getAbsolutePath());
            loadOrGenerateKeyPair();
            Log.i(TAG, "SimpleAdbManager initialized successfully");
        }

        private void loadOrGenerateKeyPair() throws Exception {
            Log.i(TAG, "Loading or generating key pair");
            if (keyFile.exists() && pubKeyFile.exists() && certFile.exists()) {
                Log.i(TAG, "Loading existing key pair and certificate");
                try {
                    // Load private key
                    byte[] privateKeyBytes = readFileBytes(keyFile);
                    PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    privateKey = keyFactory.generatePrivate(privateSpec);
                    Log.i(TAG, "Private key loaded");

                    // Load public key
                    byte[] publicKeyBytes = readFileBytes(pubKeyFile);
                    X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
                    publicKey = keyFactory.generatePublic(publicSpec);
                    Log.i(TAG, "Public key loaded");

                    // Load certificate
                    byte[] certBytes = readFileBytes(certFile);
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
                    Log.i(TAG, "Certificate loaded");

                } catch (Exception e) {
                    Log.e(TAG, "Failed to load existing keys, generating new ones", e);
                    generateNewKeyPairAndCert();
                }

            } else {
                Log.i(TAG, "No existing keys found, generating new ones");
                generateNewKeyPairAndCert();
            }
        }

        private byte[] readFileBytes(File file) throws IOException {
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int bytesRead = fis.read(bytes);
                if (bytesRead != bytes.length) {
                    throw new IOException("Failed to read entire file");
                }
            }
            return bytes;
        }

        private void writeFileBytes(File file, byte[] bytes) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
        }

        private void generateNewKeyPairAndCert() throws Exception {
            Log.i(TAG, "Generating new RSA key pair");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            Log.i(TAG, "Key pair generated");

            // Generate self-signed certificate
            Log.i(TAG, "Generating self-signed certificate");
            certificate = generateSelfSignedCertificate(keyPair);
            Log.i(TAG, "Certificate generated");

            // Save keys
            Log.i(TAG, "Saving keys to files");
            writeFileBytes(keyFile, privateKey.getEncoded());
            writeFileBytes(pubKeyFile, publicKey.getEncoded());
            writeFileBytes(certFile, certificate.getEncoded());
            Log.i(TAG, "Keys and certificate saved successfully");
        }

        private X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
            X500Name issuer = new X500Name("CN=ADBAutoEnable");
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
            Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    issuer,
                    serial,
                    notBefore,
                    notAfter,
                    issuer,
                    publicKeyInfo
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(keyPair.getPrivate());

            X509CertificateHolder certHolder = certBuilder.build(signer);
            return new JcaX509CertificateConverter()
                    .getCertificate(certHolder);
        }

        @Override
        protected PrivateKey getPrivateKey() {
            return privateKey;
        }

        @Override
        protected Certificate getCertificate() {
            return certificate;
        }

        @Override
        protected String getDeviceName() {
            return "ADBAutoEnable";
        }
    }
}
