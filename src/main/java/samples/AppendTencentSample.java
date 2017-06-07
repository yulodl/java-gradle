package samples;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * This samples demonstrates how to upload an object by append mode
 * to Tencent COS using Java.
 */
public class AppendTencentSample {

    private static final String secretId = "AKID78ygNxIYaSEpFVppkV7XqyueLrDe2EJD";
    private static final String secretKey = "skU5gDZlmcMyTxNgqtoeM6jEdu463sbC";
    //private static final String appId = "1252375653";
    private static HttpClient httpClient;

    public static void main(String[] args) throws Exception {
        HttpClientBuilder hcb = HttpClientBuilder.create();
        hcb.setHttpProcessor(new ImmutableHttpProcessor(new HttpRequestInterceptor[]{new RequestContent(true), new RequestTargetHost()}));
        httpClient = hcb.build();
        get();
        //put();
        //append();
    }
    public static void get() throws Exception{
        HttpGet httpGet = new HttpGet("http://yuxd-1252375653.costj.myqcloud.com/favicon.ico");
        HttpResponse httpResponse = httpClient.execute(httpGet);
        System.out.println(httpResponse.getStatusLine());
        for (Header header : httpResponse.getAllHeaders()) {
            System.out.println(header.toString());
        }
        System.out.println(EntityUtils.toString(httpResponse.getEntity()));
    }
    public static void put() throws Exception{
        HttpPut httpPut = new HttpPut("http://yuxd-1252375653.cn-north.myqcloud.com/put.ts");
        FileEntity entity = new FileEntity(new File("/Users/yuxd/Downloads/wm1.ts"));
        httpPut.setEntity(entity);
        httpPut.setHeader(getAuthorationHeader(httpPut));
        HttpResponse httpResponse = httpClient.execute(httpPut);
        System.out.println(httpResponse.getStatusLine());
        for (Header header : httpResponse.getAllHeaders()) {
            System.out.println(header.toString());
        }
        System.out.println(EntityUtils.toString(httpResponse.getEntity()));
    }
    public static void append() throws Exception{
        /*
         * Append an object from specfied input stream, keep in mind that
         * position should be set to zero at first time.
         */
        FileInputStream fis = new FileInputStream("/Users/yuxd/Downloads/wm1.ts");
        long off = 0L;
        int len = 1048576; //1M

        byte[] b = new byte[len];
        fis.skip(off);
        fis.read(b, 0, len);
        HttpPost httpPost = new HttpPost("http://yuxd-1252375653.cn-north.myqcloud.com/append.ts?append&position=" + off);
        httpPost.setEntity(new ByteArrayEntity(b));
        httpPost.setHeader(getAuthorationHeader(httpPost));
        HttpResponse httpResponse = httpClient.execute(httpPost);
        System.out.println(httpResponse.getStatusLine());
        for (Header header : httpResponse.getAllHeaders()) {
            System.out.println(header.toString());
        }
        System.out.println(EntityUtils.toString(httpResponse.getEntity()));
    }

    private static Header getAuthorationHeader(HttpEntityEnclosingRequestBase httpRequest) throws Exception{
        httpRequest.setHeader("Content-Length", httpRequest.getEntity().getContentLength() + "");
        httpRequest.setHeader("Host", "yuxd-1252375653.cn-north.myqcloud.com");
        //SignKey
        Date now = new Date();
        long start = now.getTime()/1000;
        long end = start + 60 * 60 * 24;
        String signTime = start + ";" + end;
        String signKey = HmacUtils.hmacSha1Hex(secretKey, signTime);
        System.out.println(signKey);
        //FormatString
        StringBuffer fs = new StringBuffer();
        fs.append(httpRequest.getMethod().toLowerCase() + "\n");
        fs.append(httpRequest.getURI().getPath() + "\n");
        String queryStr = httpRequest.getURI().getQuery();
        fs.append((queryStr == null ? "" : queryStr) + "\n");
        Header[] headers = httpRequest.getAllHeaders();
        Arrays.sort(headers, new Comparator<Header>() {
            @Override
            public int compare(Header o1, Header o2) {
                int res = String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
                if (res == 0) {
                    res = o1.getName().compareTo(o2.getName());
                }
                return res;
            }
        });
        String and = "";
        for (Header h : headers) {
            fs.append(and);
            fs.append(h.getName().toLowerCase() + "=" + URLEncoder.encode(h.getValue(), "UTF-8"));
            if(and.isEmpty()) and = "&";
        }
        fs.append("\n");
        System.out.println(fs.toString());
        String hashedFs = DigestUtils.sha1Hex(fs.toString());
        System.out.println(hashedFs);
        //StringToSign
        StringBuffer sts = new StringBuffer();
        sts.append("sha1\n");
        sts.append(signTime + "\n");
        sts.append(hashedFs + "\n");
        //Signature
        System.out.println(sts.toString());
        String sign = HmacUtils.hmacSha1Hex(signKey, sts.toString());
        StringBuffer auth = new StringBuffer();
        auth.append("q-sign-algorithm=sha1");
        auth.append("&q-ak=" + secretId);
        auth.append("&q-sign-time=" + signTime);
        auth.append("&q-key-time=" + signTime);
        auth.append("&q-header-list=");
        String semicolon = "";
        for (Header h : headers) {
            auth.append(semicolon);
            auth.append(h.getName().toLowerCase());
            if(semicolon.isEmpty()) semicolon = ";";
        }
        auth.append("&q-url-param-list=");
        if (queryStr != null) {
            String paramList = queryStr.replaceAll("=[^&]*", "");
            paramList = paramList.replaceAll("&", ";");
            auth.append(paramList);
        }
        auth.append("&q-signature=" + sign);

        httpRequest.removeHeaders("Content-Length");
        httpRequest.removeHeaders("Host");

        System.out.println(auth.toString());
        return new BasicHeader("Authorization", auth.toString());
    }

}
