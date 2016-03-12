package HTTPServer;


import HTTPServer.processors.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.zip.*;

import static HTTPServer.HttpRequest.*;


public class Client implements Runnable {
    private Socket socket;
    private FileManager fm;

    public Client(Socket socket, String path) {
        this.socket = socket;
        fm = new FileManager(path);
    }

    private void returnStatusCode(int code, OutputStream os) throws IOException {
        String msg = null;

        switch (code) {
            case 400:
                msg = "HTTP/1.1 400 Bad Request";
                break;
            case 404:
                msg = "HTTP/1.1 404 Not Found";
                break;
            case 500:
                msg = "HTTP/1.1 500 Internal Server Error";
                break;
        }

        byte[] resp = msg.concat("\r\n\r\n").getBytes();
        os.write(resp);
    }

    private byte[] getBinaryHeaders(List<String> headers) {
        StringBuilder res = new StringBuilder();

        for (String s : headers)
            res.append(s);

        return res.toString().getBytes();
    }

    private void process(byte[] request, OutputStream os) throws IOException {
        HttpRequest req = parseRequest(request);

        if (!req.isFull()) {
            return;
        }
        if (req.getStatusCode() != 200) {
            returnStatusCode(req.getStatusCode(), os);
            return;
        }

        String url = req.getUrl();
        if ("/".equals(url)) {
            url = "/index.html";
        }
        List<String> headers = new ArrayList<String>();
        headers.add("HTTP/1.1 200 OK\r\n");

        if (req.getMethod().equalsIgnoreCase("POST")) {

            Collection<Part> files = req.getParts();

            String zipName = String.valueOf(System.currentTimeMillis()) + ".zip";
            writeFilesToZip(files, zipName);

            byte[] zipFile = fm.get("/" + zipName);

            addHeadersForZipFile(headers, zipFile);

            os.write(getBinaryHeaders(headers));
            os.write(zipFile);
            return;
        }

        byte[] content = fm.get(url);
        if (content == null) {
            returnStatusCode(404, os);
            return;
        }

        ProcessorsList pl = new ProcessorsList();
        pl.add(new Compressor(6));
        pl.add(new Chunker(30)); // comment
        content = pl.process(content, headers);

        if (content == null) {
            returnStatusCode(500, os);
            return;
        }

        headers.add("Connection: close\r\n\r\n");

        os.write(getBinaryHeaders(headers));
        os.write(content);
    }

    private void writeFilesToZip(Collection<Part> partCollection, String zipName) throws IOException {
        FileOutputStream outNewZip = new FileOutputStream("D://" + zipName);
        ZipOutputStream out = new ZipOutputStream(outNewZip);
        for (Part p : partCollection) {
            if (p.getContent() != null) {
                ZipEntry ze = new ZipEntry(p.getFileName());
                out.putNextEntry(ze);
                out.write(p.getContent());
                out.closeEntry();
            }
        }
        out.close();
    }

    private void addHeadersForZipFile(List<String> headers, byte[] zipFile) {
        headers.add("Accept-Ranges: bytes\r\n");
        headers.add("Content-Length: " + zipFile.length + "\r\n");
        headers.add("Content-Type: application/zip\r\n");
        headers.add("Content-Disposition: attachment; filename=archive.zip\r\n");
        headers.add("Connection: close\r\n\r\n");
    }


    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            byte[] buf, temp = null;
            int len, b;

            try {
                do {
                    len = is.available();
                    buf = new byte[len];

                    if (is.read(buf) > 0) {
                        bs.write(buf);
                    }
                    temp = bs.toByteArray();

                    for (int i = 0; i < temp.length - 3; i++) {
                        if ((temp[i] == (byte) 13) && (temp[i + 1] == (byte) 10) &&
                                (temp[i + 2] == (byte) 13) && (temp[i + 3] == (byte) 10)) {
                            process(temp, os);
                        }
                    }
                } while ( ! Thread.currentThread().isInterrupted());
            } finally {
                socket.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}