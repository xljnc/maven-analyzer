package com.wt.maven.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * httpclient 连接池
 */
@Slf4j
public class HttpClientUtil {

    static CloseableHttpClient httpClient;
    /**
     * 超时时间，单位:秒
     */
    private static int timeout = 10;
    /**
     * 最大连接数
     */
    private static int maxTotal = 200;
    /**
     * 单Url最大连接数
     */
    private static int maxPerRoute = 20;
    /**
     * 扫描连接池间隔,单位:秒
     */
    private static int scanTime = 5;
    /**
     * 多久空闲的连接将被清除，单位:秒
     */
    private static int idleTime = 30;
    private static RequestConfig requestConfig;
    private static PoolingHttpClientConnectionManager cm;
    private static ScheduledExecutorService monitorExecutor;

    /**
     * 连接池配置
     */
    private static CloseableHttpClient config() {
        requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .setConnectTimeout(timeout * 1000)
                .build();
        // 初始化连接池，可用于请求HTTP/HTTPS（信任所有证书）
        cm = new PoolingHttpClientConnectionManager(getRegistry());
        // 整个连接池最大连接数
        cm.setMaxTotal(maxTotal);
        // 每路由最大连接数，默认值是2
        cm.setDefaultMaxPerRoute(maxPerRoute);
        monitorExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("ClearHttpThread");
                t.setDaemon(true);
                return t;
            }
        });
        monitorExecutor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //关闭异常连接
                cm.closeExpiredConnections();
                //关闭5s空闲连接
                cm.closeIdleConnections(idleTime, TimeUnit.SECONDS);
                log.debug("close expired and idle for over 5s connection");
            }
        }, 5, scanTime, TimeUnit.SECONDS);
        return HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(requestConfig).
                setRetryHandler(new DefaultHttpRequestRetryHandler(0, false)).build();
    }

    private static CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (HttpClientUtil.class) {
                if (httpClient == null) {
                    httpClient = config();
                }
            }
        }
        return httpClient;
    }

    /**
     * 个性化设置相关参数，可以不调用，使用缺省值
     *
     * @param timeout     超时值,单位:秒
     * @param maxTotal    最大连接数
     * @param maxPerRoute 单Url最大连接数
     * @param scanTime    扫描连接池间隔,单位:秒
     * @param idleTime    多久空闲的连接将被清除，单位:秒
     */
    public synchronized static void updateConfig(int timeout, int maxTotal, int maxPerRoute,
                                                 int scanTime, int idleTime) {
        if (httpClient != null) {
            try {
                httpClient.close();
                cm.close();
                monitorExecutor.shutdown();
                httpClient = null;
            } catch (IOException e) {
                log.error("httpclient close error", e);
            }
            HttpClientUtil.timeout = timeout;
            HttpClientUtil.maxTotal = maxTotal;
            HttpClientUtil.maxPerRoute = maxPerRoute;
            HttpClientUtil.scanTime = scanTime;
            HttpClientUtil.idleTime = idleTime;
        }
    }

    /**
     * 发送 HTTP 请求
     *
     * @param request
     * @return
     * @throws Exception
     */
    public static String doHttp(HttpRequestBase request) {
        try {
            return doRequest(getHttpClient(), request);
        } catch (Exception e) {
            log.error("doHttp.error.request.uri" + request.getURI(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 处理Http/Https请求，并返回请求结果
     * <p>注：默认请求编码方式 UTF-8</p>
     *
     * @param httpClient
     * @param request
     * @return
     * @throws Exception
     */
    private static String doRequest(CloseableHttpClient httpClient, HttpRequestBase request)
            throws Exception {
        String result;
        CloseableHttpResponse response = null;
        try {
            // 获取请求结果
            response = httpClient.execute(request);
            // 解析请求结果
            HttpEntity entity = response.getEntity();
            // 转换结果
            result = EntityUtils.toString(entity, StandardCharsets.UTF_8.name());
            // 关闭IO流
            EntityUtils.consume(entity);
        } finally {
            if (null != response) {
                response.close();
            }
        }
        return result;
    }

    /**
     * 发送HTTP GET请求
     */
    public static String httpGet(String url, Map<String, String> param) throws Exception {
       return httpGet(url, param, null);
    }

    /**
     * 发送HTTP GET请求
     */
    public static String httpGet(String url, Map<String, String> param, Map<String, String> header) throws Exception {
        if (param != null && !param.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : param.entrySet()) {
                if (first) {
                    first = false;
                    url += "?";
                } else {
                    url += "&";
                }
                url += entry.getKey() + "=" + entry.getValue();
            }
        }
        HttpGet httpGet = new HttpGet(url);
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return doHttp(httpGet);
    }

    /**
     * @param url
     * @param body
     * @param contentType 请求格式 ，如:application/json
     * @param charset     编码类型 ，如:StandardCharsets.UTF_8
     * @return
     * @throws Exception
     */
    public static String httpPost(String url, String body, String contentType, Charset charset)
            throws Exception {
        HttpPost httpPost = new HttpPost(url);
        // 设置请求头
        httpPost.addHeader("Content-Type", contentType);
        // 设置请求参数
        httpPost.setEntity(new StringEntity(body, charset.name()));
        return doHttp(httpPost);
    }

    public static String httpPostJson(String url, String JsonStr) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        // 设置请求头
        httpPost.addHeader("Content-Type", "application/json");
        // 设置请求参数
        httpPost.setEntity(new StringEntity(JsonStr, StandardCharsets.UTF_8));
        return doHttp(httpPost);
    }

    /**
     * dns解析
     */
    private static DnsResolver prepareProxiedDnsResolver() {
        return new SystemDefaultDnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                log.error("DnsResolver.resolve.UnknownHostException======");
                return super.resolve(host);
            }
        };
    }

    /**
     * 获取 HTTPClient注册器
     *
     * @return
     * @throws Exception
     */
    private static Registry<ConnectionSocketFactory> getRegistry() {
        Registry<ConnectionSocketFactory> registry = null;
        try {
            registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory()).register(
                            "https", getSSLFactory()).build();
        } catch (Exception e) {
            log.error("获取 HTTPClient注册器失败", e);
        }
        return registry;
    }

    /**
     * 获取HTTPS SSL连接工厂
     * <p>跳过证书校验，即信任所有证书</p>
     *
     * @return
     * @throws Exception
     */
    private static SSLConnectionSocketFactory getSSLFactory() throws Exception {
        // 设置HTTPS SSL证书信息，跳过证书校验，即信任所有证书请求HTTPS
        SSLContextBuilder sslBuilder = new SSLContextBuilder()
                .loadTrustMaterial(null, new TrustAllStrategy());
        // 获取HTTPS SSL证书连接上下文
        SSLContext sslContext = sslBuilder.build();
        // 获取HTTPS连接工厂
        SSLConnectionSocketFactory sslCsf = new SSLConnectionSocketFactory(sslContext,
                new String[]{"SSLv2Hello", "SSLv3", "TLSv1","TLSv1.1", "TLSv1.2","TLSv1.3"}, null,
                NoopHostnameVerifier.INSTANCE);
        return sslCsf;
    }


    /**
     * 下载文件 到指定文件路径上 支持 curl xxxx 的这种指令
     */
    public static String download(String url, String filepath) {
        try {
            CloseableHttpClient client = getHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = client.execute(httpget);

            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();

            File file = new File(filepath);
            file.getParentFile().mkdirs();
            FileOutputStream fileout = new FileOutputStream(file);
            /**
             * 根据实际运行效果 设置缓冲区大小
             */
            byte[] buffer = new byte[1024];
            int ch = 0;
            while ((ch = is.read(buffer)) != -1) {
                fileout.write(buffer, 0, ch);
            }
            is.close();
            fileout.flush();
            fileout.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        //get
        String content = HttpClientUtil.httpGet("https://come-future.com", null);
        System.out.println(content);
        //post
        HttpClientUtil.httpPostJson("https://come-future.com", "{ \"name\": \"babo\"}");
        HttpClientUtil.updateConfig(2, 20, 10, 60, 5);
        System.out.println("---------------------------------");
        System.out.println("---------------------------------");
        System.out.println("---------------------------------");
        System.out.println(HttpClientUtil.httpGet("https://come-future.com", null));
    }
}