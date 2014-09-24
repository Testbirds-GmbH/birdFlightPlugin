package birdflight;

import java.io.*;
import java.util.Scanner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * A testflight uploader
 */
public class BirdflightUploader implements Serializable {

    static interface Logger {

        void logDebug(String message);
    }

    static class UploadRequest implements Serializable {

        String filePaths;
        String dsymPath;
        String apiToken;
        String packIdentifier;
        String buildIdentifier;
        // String teamToken;
        String version;
        // Boolean notifyTeam;
        String buildNotes;
        String compatibility;
        String distribution;
        Boolean isPublic;
        File file;
        File dsymFile;
        String lists;
        Boolean replace;
        Boolean debug;

        public String toString() {
            return new ToStringBuilder(this).append("filePaths", filePaths)
                    .append("dsymPath", dsymPath).append("version", version)
                    .append("apiToken", "********")
                    .append("packageIdentfier", packIdentifier)
                    .append("buildIdentfier", buildIdentifier)
                    .append("version", version)
                    .append("compatibility", compatibility)
                    .append("distribution", distribution)
                    .append("isPublic", isPublic)
                    .append("buildNotes", buildNotes).append("file", file)
                    .append("dsymFile", dsymFile).append("lists", lists)
                    .toString();
        }

        static UploadRequest copy(UploadRequest r) {
            UploadRequest r2 = new UploadRequest();
            // r2.filePaths = r.filePaths;
            r2.dsymPath = r.dsymPath;
            r2.apiToken = r.apiToken;
            r2.buildIdentifier = r.buildIdentifier;
            r2.version = r.version;
            r2.buildNotes = r.buildNotes;
            r2.compatibility = r.compatibility;
            r2.distribution = r.distribution;
            r2.isPublic = r.isPublic;
            r2.packIdentifier = r.packIdentifier;

            r2.file = r.file;
            r2.dsymFile = r.dsymFile;
            r2.lists = r.lists;
            r2.replace = r.replace;
            r2.debug = r.debug;

            return r2;
        }
    }

    private Logger logger = null;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public String upload(UploadRequest ur) throws IOException,
            org.json.simple.parser.ParseException {

        HttpResponse response = send(ur);

        HttpEntity resEntity = response.getEntity();

        InputStream is = resEntity.getContent();
        // // Improved error handling.
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("statusCode " + statusCode);

        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        System.out.println("Answer: " + writer.toString());
        logDebug("POST Answer: " + writer.toString());
        return writer.toString();
    }

    public HttpResponse send(UploadRequest ur) throws IOException,
            org.json.simple.parser.ParseException {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpPost httpPost = new HttpPost("https://www.birdflightapp.com/apps/" + ur.packIdentifier + "/app-builds/new.json?apikey=" + ur.apiToken);
        System.out.println(httpPost.getURI().toString());
        FileBody fileBody = new FileBody(ur.file);

        MultipartEntity entity = new MultipartEntity();

        entity.addPart("id", new StringBody(ur.buildIdentifier));
        entity.addPart("version", new StringBody(ur.version));
        entity.addPart("notes", new StringBody(ur.buildNotes));
        entity.addPart("compatibility", new StringBody(ur.compatibility));
        entity.addPart("appBuildFile", fileBody);
        if (ur.distribution != null) {
            entity.addPart("distribution", new StringBody(ur.distribution));
        }
        if (ur.dsymFile != null) {
            FileBody dsymFileBody = new FileBody(ur.dsymFile);
            entity.addPart("dsymBuildFile", dsymFileBody);
        }

        httpPost.setEntity(entity);

        HttpResponse response = httpClient.execute(httpPost);
        return response;
    }

    private void logDebug(String message) {
        if (logger != null) {
            logger.logDebug(message);
        }
    }
}
