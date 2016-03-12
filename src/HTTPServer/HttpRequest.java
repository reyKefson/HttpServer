package HTTPServer;

import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class HttpRequest {
    private final byte[] request;
    private String header;
    private String method;
    private String url;
    private String version;
    private byte[] boundary;
    private int contentLength;
    private boolean full = true;
    private int statusCode;
    private Set<Part> set = new HashSet<Part>();

    private HttpRequest(byte[] request) {
        this.request = request;
    }

    public static HttpRequest parseRequest(byte[] request){
        HttpRequest obj = new HttpRequest(request);
        obj.parse();
        return obj;
    }

    private void parse(){
        int idxEndHead = indexOfEndHead(request, 0);
        byte[] headerByte = new byte[idxEndHead];
        System.arraycopy(request, 0, headerByte, 0, idxEndHead);
        header = new String(headerByte);

        String startLine = this.header.substring(0, this.header.indexOf("\r\n"));
        String[] parts = startLine.split(" ");
        if (parts.length != 3) {
            this.setStatusCode(400);
            return;
        }
        this.method = parts[0];
        this.url = parts[1];
        this.version = parts[2];

        if ((!this.getVersion().equalsIgnoreCase("HTTP/1.0"))
                && (!this.getVersion().equalsIgnoreCase("HTTP/1.1"))) {
            this.setStatusCode(400);
            return;
        }
        if (this.method.equalsIgnoreCase("GET")) {
            this.setStatusCode(200);
        } else if (this.method.equalsIgnoreCase("POST")){
            boundary =  getParameter(header, "boundary=", "\n").getBytes();
            contentLength = Integer.parseInt(getParameter(header, "Content-Length:", "\n"));

            if ((header.length() + contentLength) != request.length) {
                full = false;
                return;
            }

            parseContent(headerByte.length);

            this.setStatusCode(200);
        } else {
            this.setStatusCode(400);
        }
    }

    private void parseContent(int beginIndex){
        byte[] realBoundary = ("--" + new String(boundary)).getBytes();
        byte[] endBoundary = (new String(realBoundary) + "--\r\n").getBytes();
        String newLine = "\r\n";
        int off = beginIndex;

        while ((off + endBoundary.length - beginIndex) != contentLength) {
            Part part = new Part();
            int startSubHeader = indexOf(request, realBoundary, off) + realBoundary.length;
            int endSubHeader = indexOfEndHead(request, startSubHeader);
            String subHeader = new String(request, startSubHeader, endSubHeader - startSubHeader);

            String fileName = getParameter(subHeader, "filename=\"", "\"");
            if (fileName.length() != 0) {
                part.setFileName(fileName);
            }

            int endCurrentFile = indexOf(request, realBoundary, endSubHeader);

            byte[] file = ArrayUtils.subarray(request, endSubHeader, endCurrentFile - newLine.length());
            if (file.length != 0) {
                part.setContent(file);
            }
            set.add(part);
            off = endCurrentFile;
        }
    }

    private String getParameter(String headers, String beginSprt, String endSprt) {
        int start = headers.indexOf(beginSprt) + beginSprt.length();
        int end = headers.indexOf(endSprt, start);
        return headers.substring(start, end).trim();
    }

    private static int indexOf(byte[] array, byte[] target, int fromIndex) {
        if(target.length == 0) {
            return 0;
        } else {
            label28:
            for(int i = fromIndex; i < array.length - target.length + 1; ++i) {
                for(int j = 0; j < target.length; ++j) {
                    if(array[i + j] != target[j]) {
                        continue label28;
                    }
                }

                return i;
            }
            return -1;
        }
    }

    public Collection<Part> getParts(){
        return set;
    }

    private int indexOfEndHead(byte[] request, int startPos) {
        for (int i = startPos; i < request.length - 3; i++){
            if ((request[i] == (byte) 13) && (request[i + 1] == (byte) 10) &&
                    (request[i + 2] == (byte) 13) && (request[i + 3] == (byte) 10)) {
                return i + 4;
            }
        }
        return -1;
    }

    public String getHeader() {
        return header;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public byte[] getBoundary() {
        return boundary;
    }

    public int getContentLength() {
        return contentLength;
    }

    public boolean isFull() {
        return full;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HttpRequest{");
        sb.append("boundary='").append(boundary).append('\n');
        sb.append(", header='").append(header).append('\n');
        sb.append(", method='").append(method).append('\n');
        sb.append(", url='").append(url).append('\n');
        sb.append(", version='").append(version).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
