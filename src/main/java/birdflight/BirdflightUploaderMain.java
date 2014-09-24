package birdflight;

import java.io.File;

public class BirdflightUploaderMain {
    /**
     * Useful for testing
     */
    public static void main(String[] args) {
        try {
            upload(args);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
    }

    private static void upload(String[] args) throws Exception {
        BirdflightUploader uploader = new BirdflightUploader();
        uploader.setLogger(new BirdflightUploader.Logger() {
            public void logDebug(String message) {
                System.out.println(message);
            }
        });

        BirdflightUploader.UploadRequest r = new BirdflightUploader.UploadRequest();
        r.apiToken = "11";
        r.packIdentifier = "1";
        r.buildNotes = "im a bird";
        r.version = "12121212";
        r.distribution = "2";
        r.compatibility = "ios v.5";
        r.buildIdentifier = "ident1";
        r.isPublic = true;
        File f = new File("/file to apk.apk");
        r.file = f;

        uploader.upload(r);
    }
}
